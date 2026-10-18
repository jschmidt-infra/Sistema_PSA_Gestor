package br.unisc.psagestor.acesso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.unisc.psagestor.acesso.model.*;
import br.unisc.psagestor.acesso.repository.UsuarioRepository;
import br.unisc.psagestor.acesso.service.EventosAcesso;
import br.unisc.psagestor.acesso.service.LimiteLogin;
import br.unisc.psagestor.acesso.service.LimiteLogin.Bloqueado;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.junit.jupiter.api.extension.ExtendWith(OutputCaptureExtension.class)
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @Autowired LimiteLogin limite;
    @Autowired EventosAcesso eventos;
    @Autowired ServletListenerRegistrationBean<HttpSessionListener> auditoriaEncerramentoSessao;
    private static final String EMAIL = "teste@example.test";
    private static final String SENHA = "Senha-so-de-teste-123";

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM tentativa_login");
        jdbc.update("DELETE FROM evento_acesso");
        jdbc.update("DELETE FROM auditoria_evento");
        jdbc.update("UPDATE usuario SET atualizado_por = NULL, criado_por = NULL");
        usuarios.deleteAll();
        usuarios.saveAndFlush(
                new Usuario(
                        "Teste", EMAIL, encoder.encode(SENHA), Perfil.ADMINISTRADOR, null, null));
    }

    record Token(MockHttpSession session, String header, String value) {}

    private Token token(MockHttpSession session) throws Exception {
        var request = get("/api/v1/auth/csrf");
        if (session != null) request.session(session);
        var result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        return new Token(
                (MockHttpSession) result.getRequest().getSession(),
                body.get("headerName").asText(),
                body.get("token").asText());
    }

    private ResultActions entrar(Token token, String senha) throws Exception {
        return mvc.perform(
                post("/api/v1/auth/login")
                        .session(token.session())
                        .header(token.header(), token.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", EMAIL, "senha", senha))));
    }

    private MockHttpSession entrar() throws Exception {
        var csrf = token(null);
        entrar(csrf, SENHA).andExpect(status().isOk());
        return csrf.session();
    }

    long eventos(String evento, String motivo) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM evento_acesso WHERE evento=? AND motivo=?",
                Long.class,
                evento,
                motivo);
    }

    @Test
    void auditaLoginRecusasBloqueioLogoutSemCredenciais(CapturedOutput output) throws Exception {
        String segredo = "SEGREDO-NAO-REGISTRAR-987654";
        var csrf = token(null);
        for (int i = 0; i < 5; i++)
            entrar(csrf, segredo).andExpect(status().is(i == 4 ? 429 : 401));
        entrar(csrf, SENHA).andExpect(status().isTooManyRequests());
        assertThat(eventos("LOGIN_RECUSADO", "CREDENCIAIS_RECUSADAS")).isEqualTo(4);
        assertThat(eventos("LOGIN_RECUSADO", "BLOQUEIO_TEMPORARIO")).isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM evento_acesso WHERE usuario_id IS NOT NULL",
                                Long.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM evento_acesso WHERE requisicao_id IS NULL",
                                Long.class))
                .isZero();
        jdbc.update(
                "UPDATE tentativa_login SET bloqueado_ate=TIMESTAMPADD(SECOND,-1,CURRENT_TIMESTAMP(6))");
        var session = entrar();
        assertThat(eventos("LOGIN_SUCESSO", "AUTENTICADO")).isEqualTo(1);
        var novo = token(session);
        mvc.perform(
                        post("/api/v1/auth/logout")
                                .session(session)
                                .header(novo.header(), novo.value()))
                .andExpect(status().isNoContent());
        assertThat(eventos("SESSAO_ENCERRADA", "LOGOUT")).isEqualTo(1);
        String dados = jdbc.queryForList("SELECT * FROM evento_acesso").toString();
        assertThat(dados)
                .doesNotContain(
                        EMAIL,
                        SENHA,
                        segredo,
                        csrf.value(),
                        novo.value(),
                        "sessionId",
                        "PSA_SESSION",
                        "senha_hash");
        assertThat(output.getAll())
                .doesNotContain(EMAIL, SENHA, segredo, csrf.value(), novo.value(), "PSA_SESSION");
    }

    @Test
    void auditaRejeicaoDeCsrfEEntradaInvalidaSemLerCorpo() throws Exception {
        mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden());
        assertThat(eventos("LOGIN_RECUSADO", "CSRF_OU_ACESSO_NEGADO")).isEqualTo(1);
        var csrf = token(null);
        mvc.perform(
                        post("/api/v1/auth/login")
                                .session(csrf.session())
                                .header(csrf.header(), csrf.value())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(eventos("LOGIN_RECUSADO", "REQUISICAO_INVALIDA")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tentativa_login", Long.class))
                .isZero();
    }

    @Test
    void listenerRegistraExpiracaoUmaVezENaoRegistraSessaoAnonima() throws Exception {
        var session = entrar();
        var listener = auditoriaEncerramentoSessao.getListener();
        listener.sessionDestroyed(new HttpSessionEvent(session));
        listener.sessionDestroyed(new HttpSessionEvent(session));
        listener.sessionDestroyed(new HttpSessionEvent(token(null).session()));
        assertThat(eventos("SESSAO_ENCERRADA", "EXPIRACAO_OU_ENCERRAMENTO_CONTAINER")).isEqualTo(1);
    }

    @Test
    void autenticaComCsrfERenovaSessaoSemExporSenha() throws Exception {
        var csrf = token(null);
        var anterior = csrf.session().getId();
        entrar(csrf, SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("perfil").value("ADMINISTRADOR"))
                .andExpect(jsonPath("senhaHash").doesNotExist());
        assertThat(csrf.session().getId()).isNotEqualTo(anterior);
        mvc.perform(get("/api/v1/auth/me").session(csrf.session())).andExpect(status().isOk());
        mvc.perform(
                        post("/api/v1/auth/logout")
                                .session(csrf.session())
                                .header(csrf.header(), csrf.value()))
                .andExpect(status().isForbidden());
        var renovado = token(csrf.session());
        mvc.perform(
                        post("/api/v1/auth/logout")
                                .session(renovado.session())
                                .header(renovado.header(), renovado.value()))
                .andExpect(status().isNoContent());
        assertThat(renovado.session().isInvalid()).isTrue();
    }

    @Test
    void bloqueiaLoginSemCsrfEConsultaAnonima() throws Exception {
        mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of("email", EMAIL, "senha", SENHA))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void recusaSenhaIncorreta() throws Exception {
        entrar(token(null), "senha-errada").andExpect(status().isUnauthorized());
    }

    @Test
    void cincoFalhasBloqueiamPorUmaHoraSemProrrogarEAposExpirarPermiteLogin() throws Exception {
        for (int i = 0; i < 4; i++)
            entrar(token(null), "errada").andExpect(status().isUnauthorized());
        entrar(token(null), "errada")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "3600"))
                .andExpect(jsonPath("codigo").value("LOGIN_TEMPORARIAMENTE_BLOQUEADO"));
        var prazo =
                jdbc.queryForObject("SELECT bloqueado_ate FROM tentativa_login", Timestamp.class);
        entrar(token(null), SENHA).andExpect(status().isTooManyRequests());
        entrar(token(null), "errada").andExpect(status().isTooManyRequests());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT bloqueado_ate FROM tentativa_login", Timestamp.class))
                .isEqualTo(prazo);
        assertThat(jdbc.queryForObject("SELECT falhas FROM tentativa_login", Integer.class))
                .isEqualTo(5);
        jdbc.update(
                "UPDATE tentativa_login SET bloqueado_ate=TIMESTAMPADD(SECOND,-1,CURRENT_TIMESTAMP(6))");
        entrar(token(null), SENHA).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT falhas FROM tentativa_login", Integer.class))
                .isZero();
    }

    @Test
    void loginCorretoZeraFalhasEVariacaoDeMaiusculasNaoContornaLimite() throws Exception {
        for (int i = 0; i < 4; i++)
            entrar(token(null), "errada").andExpect(status().isUnauthorized());
        entrar(token(null), SENHA).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT falhas FROM tentativa_login", Integer.class))
                .isZero();
        for (int i = 0; i < 5; i++) {
            var csrf = token(null);
            mvc.perform(
                            post("/api/v1/auth/login")
                                    .session(csrf.session())
                                    .header(csrf.header(), csrf.value())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            json.writeValueAsString(
                                                    Map.of(
                                                            "email",
                                                            i % 2 == 0
                                                                    ? EMAIL.toUpperCase()
                                                                    : EMAIL,
                                                            "senha",
                                                            "errada"))))
                    .andExpect(status().is(i == 4 ? 429 : 401));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tentativa_login", Long.class))
                .isEqualTo(1);
    }

    @Test
    void administradorConsultaEDesbloqueiaComCsrfEAuditoriaSemReativarConta() throws Exception {
        var sessaoAdmin = entrar();
        long id = usuarios.findByEmail(EMAIL).orElseThrow().getId();
        for (int i = 0; i < 5; i++)
            entrar(token(null), "errada").andExpect(status().is(i == 4 ? 429 : 401));
        mvc.perform(get("/api/v1/usuarios/" + id + "/bloqueio-login").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("bloqueado").value(true));
        mvc.perform(delete("/api/v1/usuarios/" + id + "/bloqueio-login").session(sessaoAdmin))
                .andExpect(status().isForbidden());
        var csrf = token(sessaoAdmin);
        mvc.perform(
                        delete("/api/v1/usuarios/" + id + "/bloqueio-login")
                                .session(sessaoAdmin)
                                .header(csrf.header(), csrf.value()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/usuarios/" + id + "/bloqueio-login").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("bloqueado").value(false))
                .andExpect(jsonPath("tentativasSemSucesso").value(0));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM auditoria_evento WHERE operacao='DESBLOQUEAR_LOGIN' AND recurso_id=?",
                                Long.class,
                                id))
                .isEqualTo(1);
        entrar(token(null), SENHA).andExpect(status().isOk());
        var inativo =
                usuarios.saveAndFlush(
                        new Usuario(
                                "Inativo",
                                "inativo@example.test",
                                encoder.encode(SENHA),
                                Perfil.TECNICO,
                                null,
                                null));
        jdbc.update("UPDATE usuario SET ativo=FALSE WHERE id=?", inativo.getId());
        mvc.perform(
                        delete("/api/v1/usuarios/" + inativo.getId() + "/bloqueio-login")
                                .session(sessaoAdmin)
                                .header(csrf.header(), csrf.value()))
                .andExpect(status().isNoContent());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT ativo FROM usuario WHERE id=?",
                                Boolean.class,
                                inativo.getId()))
                .isFalse();
        mvc.perform(
                        delete("/api/v1/usuarios/9223372036854775807/bloqueio-login")
                                .session(sessaoAdmin)
                                .header(csrf.header(), csrf.value()))
                .andExpect(status().isNotFound());
        jdbc.update("UPDATE usuario SET perfil='GESTOR' WHERE id=?", id);
        var gestor = token(entrar());
        mvc.perform(
                        delete("/api/v1/usuarios/" + id + "/bloqueio-login")
                                .session(gestor.session())
                                .header(gestor.header(), gestor.value()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/usuarios/" + id + "/bloqueio-login").session(gestor.session()))
                .andExpect(status().isForbidden());
    }

    @Test
    void tentativasConcorrentesNaoUltrapassamCincoEEmailsSaoIndependentes() throws Exception {
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(8)) {
            var resultados = new ArrayList<Future<Integer>>();
            for (int i = 0; i < 8; i++)
                resultados.add(
                        executor.submit(
                                () -> {
                                    inicio.await();
                                    try {
                                        limite.autenticar(EMAIL, "errada");
                                        return 200;
                                    } catch (Bloqueado e) {
                                        return 429;
                                    } catch (BadCredentialsException e) {
                                        return 401;
                                    }
                                }));
            inicio.countDown();
            int recusas = 0, bloqueios = 0;
            for (var resultado : resultados) {
                int status = resultado.get(30, TimeUnit.SECONDS);
                if (status == 401) recusas++;
                if (status == 429) bloqueios++;
            }
            assertThat(recusas).isEqualTo(4);
            assertThat(bloqueios).isEqualTo(4);
        }
        assertThat(jdbc.queryForObject("SELECT falhas FROM tentativa_login", Integer.class))
                .isEqualTo(5);
        usuarios.saveAndFlush(
                new Usuario(
                        "Outro",
                        "outro@example.test",
                        encoder.encode(SENHA),
                        Perfil.TECNICO,
                        null,
                        null));
        assertThat(limite.autenticar("outro@example.test", SENHA).isAuthenticated()).isTrue();
        // Endereço inexistente segue o mesmo limite, sem confirmar existência de conta.
        for (int i = 0; i < 5; i++) {
            final int tentativa = i;
            Assertions.assertThatThrownBy(() -> limite.autenticar("ausente@example.test", "errada"))
                    .isInstanceOf(tentativa == 4 ? Bloqueado.class : BadCredentialsException.class);
        }
    }

    @Test
    void restringeEstadoDoBancoAoAdministrador() throws Exception {
        mvc.perform(get("/actuator/health/readiness").session(entrar())).andExpect(status().isOk());
        jdbc.update("UPDATE usuario SET perfil = 'GESTOR'");
        mvc.perform(get("/actuator/health/readiness").session(entrar()))
                .andExpect(status().isForbidden());
    }

    @Test
    void desativacaoInvalidaSessaoExistente() throws Exception {
        var session = entrar();
        jdbc.update("UPDATE usuario SET ativo = FALSE");
        mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isUnauthorized());
        assertThat(session.isInvalid()).isTrue();
        assertThat(eventos("SESSAO_ENCERRADA", "SESSAO_REVOGADA")).isEqualTo(1);
    }

    @Test
    void mudancaDePerfilInvalidaSessaoExistente() throws Exception {
        var session = entrar();
        jdbc.update("UPDATE usuario SET perfil = 'GESTOR'");
        mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void trocaDeSenhaEncerraSessaoERevogaOutras() throws Exception {
        var outra = entrar();
        var csrf = token(entrar());
        var nova = "Outra-senha-de-teste-123";
        mvc.perform(
                        put("/api/v1/auth/senha")
                                .session(csrf.session())
                                .header(csrf.header(), csrf.value())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of("senhaAtual", SENHA, "novaSenha", nova))))
                .andExpect(status().isNoContent());
        assertThat(csrf.session().isInvalid()).isTrue();
        assertThat(eventos("SESSAO_ENCERRADA", "SENHA_ALTERADA")).isEqualTo(1);
        mvc.perform(get("/api/v1/auth/me").session(outra)).andExpect(status().isUnauthorized());
        entrar(token(null), SENHA).andExpect(status().isUnauthorized());
        entrar(token(null), nova).andExpect(status().isOk());
    }
}
