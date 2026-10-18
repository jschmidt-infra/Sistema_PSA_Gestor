package br.unisc.psagestor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.unisc.psagestor.acesso.model.*;
import br.unisc.psagestor.acesso.repository.UsuarioRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.cache.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class ObservabilidadeIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @MockitoBean StringRedisTemplate redis;

    private UsernamePasswordAuthenticationToken conta(Perfil perfil) {
        var u =
                usuarios.saveAndFlush(
                        new Usuario(
                                "Observabilidade",
                                UUID.randomUUID() + "@example.test",
                                "hash-nao-usado",
                                perfil,
                                null,
                                null));
        return new UsernamePasswordAuthenticationToken(
                UsuarioSessao.de(u),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name())));
    }

    @Test
    void metricasProtegidasLogsCorrelacionadosERedisNaoDerrubaApi(CapturedOutput output)
            throws Exception {
        var admin = conta(Perfil.ADMINISTRADOR);
        var gestor = conta(Perfil.GESTOR);
        var tecnico = conta(Perfil.TECNICO);
        when(redis.opsForValue())
                .thenThrow(new IllegalStateException("CREDENCIAL-SECRETA-NUNCA-LOGAR"));
        mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health/cacheRedis")).andExpect(status().isUnauthorized());
        for (var perfil : List.of(gestor, tecnico)) {
            mvc.perform(get("/actuator/metrics/jvm.memory.used").with(authentication(perfil)))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/actuator/health").with(authentication(perfil)))
                    .andExpect(status().isForbidden());
        }
        var result =
                mvc.perform(
                                get("/api/v1/indicadores")
                                        .param("segredo", "QUERY-PRIVADA-NAO-LOGAR")
                                        .header("X-Request-ID", "ID-FORJADO-NAO-ACEITAR")
                                        .with(authentication(gestor)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("totalPago").exists())
                        .andReturn();
        String id = result.getResponse().getHeader("X-Request-ID");
        assertThat(id).matches("[0-9a-f-]{36}");
        assertThat(MDC.get("requisicaoId")).isNull();
        mvc.perform(get("/api/v1/indicadores").with(authentication(gestor)))
                .andExpect(status().isOk());
        verify(redis, times(1)).opsForValue();
        mvc.perform(get("/actuator/health/cacheRedis").with(authentication(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("DEGRADED"))
                .andExpect(jsonPath("details.estado").value("INDISPONIVEL"));
        mvc.perform(get("/actuator/health").with(authentication(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("DEGRADED"));
        mvc.perform(get("/actuator/health/readiness").with(authentication(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("UP"));
        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("components").doesNotExist());
        mvc.perform(get("/actuator/metrics/jvm.memory.used").with(authentication(admin)))
                .andExpect(status().isOk());
        mvc.perform(get("/actuator/metrics/http.server.requests").with(authentication(admin)))
                .andExpect(status().isOk());
        mvc.perform(get("/actuator/metrics/psa.cache.redis.disponivel").with(authentication(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("measurements[0].value").value(0));
        mvc.perform(get("/actuator/env").with(authentication(admin)))
                .andExpect(status().isForbidden());
        assertThat(output.getAll())
                .contains(
                        "requisicaoId=" + id,
                        "requisicao_http",
                        "rota=/api/v1/indicadores",
                        "estado=DEGRADADO")
                .doesNotContain(
                        "CREDENCIAL-SECRETA-NUNCA-LOGAR",
                        "QUERY-PRIVADA-NAO-LOGAR",
                        "ID-FORJADO-NAO-ACEITAR");
    }
}
