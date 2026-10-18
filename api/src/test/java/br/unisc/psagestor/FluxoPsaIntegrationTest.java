package br.unisc.psagestor;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.unisc.psagestor.acesso.model.*;
import br.unisc.psagestor.acesso.repository.UsuarioRepository;
import br.unisc.psagestor.arquivos.service.ArquivoService;
import br.unisc.psagestor.arquivos.service.LimpezaArquivos;
import br.unisc.psagestor.cadastros.repository.PropriedadeRepository;
import br.unisc.psagestor.comum.api.*;
import br.unisc.psagestor.configuracao.repository.ProjetoRepository;
import br.unisc.psagestor.financeiro.service.FinanceiroService;
import br.unisc.psagestor.financeiro.service.FinanceiroService.*;
import jakarta.persistence.EntityManagerFactory;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        properties = {
            "spring.datasource.url=${TEST_FLOW_DB_URL}",
            "app.storage.directory=./target/test-arquivos",
            "springdoc.api-docs.enabled=true",
            "springdoc.swagger-ui.enabled=true"
        })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FluxoPsaIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    DadosTeste db;
    @Autowired JsonMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired FinanceiroService financeiro;
    @Autowired ArquivoService arquivos;
    @Autowired LimpezaArquivos limpeza;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ProjetoRepository projetosJpa;
    @Autowired PropriedadeRepository propriedadesJpa;
    @Autowired EntityManagerFactory entityManagerFactory;
    Authentication admin, gestor, tecnico, produtor;
    long projeto, localidade, produtorId, propriedade;

    @BeforeEach
    void preparar() throws Exception {
        db = new DadosTeste(jdbc);
        jdbc.update("UPDATE configuracao_vencimento SET antecedencia_dias=30 WHERE id=1");
        for (String tabela :
                List.of(
                        "auditoria_evento",
                        "pagamento",
                        "compromisso_anual",
                        "aporte",
                        "contrato",
                        "croqui",
                        "calculo_psa",
                        "diagnostico",
                        "parametro_projeto",
                        "fonte_hidrica",
                        "visita",
                        "propriedade",
                        "arquivo_pendente")) jdbc.update("DELETE FROM " + tabela);
        jdbc.update(
                "UPDATE usuario SET produtor_id=NULL,perfil='GESTOR',criado_por=NULL,atualizado_por=NULL");
        jdbc.update("DELETE FROM produtor");
        jdbc.update("DELETE FROM usuario");
        jdbc.update("DELETE FROM projeto");
        jdbc.update("DELETE FROM localidade");
        jdbc.update("DELETE FROM sub_bacia");
        admin = conta("admin", Perfil.ADMINISTRADOR, null);
        gestor = conta("gestor", Perfil.GESTOR, null);
        tecnico = conta("tecnico", Perfil.TECNICO, null);
        projeto =
                db.inserir(
                        "INSERT INTO projeto(nome,origem_recursos,ativo) VALUES ('Teste','Simulação',TRUE)");
        localidade =
                db.inserir(
                        "INSERT INTO localidade(nome,municipio,uf) VALUES ('Teste','Teste','RS')");
        var p =
                chamar(
                        post("/api/v1/produtores"),
                        tecnico,
                        Map.of(
                                "nome",
                                "Produtor Teste",
                                "documento",
                                "12345678901",
                                "endereco",
                                "Endereço de teste",
                                "telefone",
                                "51999999999"),
                        201);
        produtorId = p.get("id").asLong();
        produtor = conta("produtor", Perfil.PRODUTOR, produtorId);
        propriedade =
                chamar(
                                post("/api/v1/propriedades"),
                                tecnico,
                                Map.of(
                                        "nome",
                                        "Área Teste",
                                        "produtorId",
                                        produtorId,
                                        "localidadeId",
                                        localidade,
                                        "areaTotal",
                                        "10.0000",
                                        "carSituacao",
                                        "NAO_POSSUI"),
                                201)
                        .get("id")
                        .asLong();
        configurar("20000.00");
    }

    @AfterEach
    void limparSessao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jpaMapeiaTodasAsTabelasERelacionamentosSemCriarOutroEsquema() {
        assertThat(entityManagerFactory.getMetamodel().getEntities()).hasSize(21);
        jdbc.update(
                "UPDATE propriedade SET criado_em='2026-01-10 08:30:00.123456' WHERE id=?",
                propriedade);
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        status -> {
                            var p = propriedadesJpa.obter(propriedade);
                            assertThat(p.getProdutor().getId()).isEqualTo(produtorId);
                            assertThat(p.getLocalidade().getId()).isEqualTo(localidade);
                            assertThat(p.getAreaTotal()).isEqualByComparingTo("10.0000");
                            assertThat(p.getFontesSituacao()).isEqualTo("NAO_AVALIADO");
                            assertThat(Dados.saida(p.dados()))
                                    .isEqualTo(Dados.saida(db.buscar("propriedade", propriedade)));
                        });
    }

    @Test
    void bloqueioJpaAtualizaEntidadeJaLidaAntesDeAlteracaoConcorrente() {
        var tx = new TransactionTemplate(transactions);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        tx.executeWithoutResult(
                status -> {
                    var anterior = projetosJpa.obter(projeto);
                    assertThat(anterior.getNome()).isEqualTo("Teste");
                    CompletableFuture.runAsync(
                                    () ->
                                            jdbc.update(
                                                    "UPDATE projeto SET nome=? WHERE id=?",
                                                    "Atualizado por outra transação",
                                                    projeto))
                            .orTimeout(10, TimeUnit.SECONDS)
                            .join();
                    var bloqueado = projetosJpa.bloquear(projeto);
                    assertThat(bloqueado.getNome()).isEqualTo("Atualizado por outra transação");
                    bloqueado.setOrigemRecursos("Origem alterada com bloqueio");
                    projetosJpa.salvarEAtualizar(bloqueado);
                    status.setRollbackOnly();
                });
        assertThat(
                        jdbc.queryForObject(
                                "SELECT nome FROM projeto WHERE id=?", String.class, projeto))
                .isEqualTo("Atualizado por outra transação");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT origem_recursos FROM projeto WHERE id=?",
                                String.class,
                                projeto))
                .isEqualTo("Simulação");
    }

    @Test
    void consultasJpaPreservamPaginacaoFiltrosEEdicaoDosCadastros() throws Exception {
        long outraPessoa =
                chamar(
                                post("/api/v1/produtores"),
                                tecnico,
                                Map.of("nome", "Outro produtor", "telefone", "51"),
                                201)
                        .get("id")
                        .asLong();
        var pagina = chamar(get("/api/v1/produtores").param("size", "1"), gestor, null, 200);
        assertThat(pagina.get("totalItems").asInt()).isEqualTo(2);
        assertThat(pagina.get("items").size()).isEqualTo(1);
        assertThat(pagina.get("items").get(0).get("id").asLong()).isEqualTo(outraPessoa);
        assertThat(
                        chamar(
                                        get("/api/v1/propriedades")
                                                .param("produtorId", Long.toString(outraPessoa)),
                                        gestor,
                                        null,
                                        200)
                                .get("totalItems")
                                .asInt())
                .isZero();
        assertThat(
                        chamar(
                                        get("/api/v1/propriedades")
                                                .param("produtorId", Long.toString(produtorId)),
                                        gestor,
                                        null,
                                        200)
                                .get("items")
                                .get(0)
                                .get("areaTotal")
                                .asText())
                .isEqualTo("10.0000");
        var fonte = Map.of("tipo", "NASCENTE", "protecao", "PROTEGIDA", "ativo", true);
        long fonteId =
                chamar(
                                post("/api/v1/propriedades/" + propriedade + "/fontes-hidricas"),
                                tecnico,
                                fonte,
                                201)
                        .get("id")
                        .asLong();
        chamar(
                put("/api/v1/fontes-hidricas/" + fonteId),
                tecnico,
                Map.of("tipo", "NASCENTE", "protecao", "PROTEGIDA", "ativo", false),
                200);
        assertThat(
                        chamar(
                                        get(
                                                "/api/v1/propriedades/"
                                                        + propriedade
                                                        + "/fontes-hidricas"),
                                        produtor,
                                        null,
                                        200)
                                .get("items")
                                .get(0)
                                .get("ativo")
                                .asBoolean())
                .isFalse();
        long visitaId =
                chamar(
                                post("/api/v1/propriedades/" + propriedade + "/visitas"),
                                tecnico,
                                Map.of(
                                        "dataVisita",
                                        LocalDate.now().toString(),
                                        "observacoes",
                                        "Visita inicial"),
                                201)
                        .get("id")
                        .asLong();
        chamar(
                put("/api/v1/visitas/" + visitaId),
                tecnico,
                Map.of(
                        "dataVisita",
                        LocalDate.now().toString(),
                        "observacoes",
                        "Observação corrigida"),
                200);
        assertThat(
                        chamar(
                                        get("/api/v1/propriedades/" + propriedade + "/visitas"),
                                        produtor,
                                        null,
                                        200)
                                .get("items")
                                .get(0)
                                .get("observacoes")
                                .asText())
                .isEqualTo("Observação corrigida");
        chamar(get("/api/v1/produtores").param("size", "0"), gestor, null, 400);
    }

    @Test
    void projecoesJpaPreservamTotaisFiltrosEDatasDoContrato() throws Exception {
        aporte("1000");
        long contratoId = contrato(), compromisso = aprovar(contratoId);
        chamar(
                post("/api/v1/pagamentos"),
                gestor,
                pagamento(compromisso, "30", "jpa-projecao-000001"),
                201);
        chamar(
                post("/api/v1/pagamentos"),
                gestor,
                pagamento(compromisso, "70", "jpa-projecao-000002"),
                201);
        var relatorio =
                chamar(
                        get("/api/v1/relatorios/aplicacao-recursos")
                                .param("projetoId", Long.toString(projeto))
                                .param("size", "1"),
                        gestor,
                        null,
                        200);
        assertThat(relatorio.get("totalItems").asInt()).isEqualTo(2);
        assertThat(relatorio.get("totalPago").asText()).isEqualTo("100.00");
        var indicadores =
                chamar(
                        get("/api/v1/indicadores").param("projetoId", Long.toString(projeto)),
                        gestor,
                        null,
                        200);
        assertThat(indicadores.get("totalPago").asText()).isEqualTo("100.00");
        assertThat(indicadores.get("mediaPorPagamento").asText()).isEqualTo("50.00");
        assertThat(indicadores.get("produtoresParticipantes").asInt()).isEqualTo(1);
        var contrato = Dados.saida(db.buscar("contrato", contratoId));
        var pagina =
                chamar(
                        get("/api/v1/contratos")
                                .param("status", "APROVADO")
                                .param("projetoId", Long.toString(projeto)),
                        gestor,
                        null,
                        200);
        assertThat(pagina.get("items").get(0).get("aprovadoEm").asText())
                .isEqualTo(contrato.get("aprovadoEm"));
        var croqui =
                Dados.saida(db.buscar("croqui", ((Number) contrato.get("croquiId")).longValue()));
        var croquis =
                chamar(
                        get("/api/v1/propriedades/" + propriedade + "/croquis"),
                        produtor,
                        null,
                        200);
        assertThat(croquis.get("items").get(0).get("criadoEm").asText())
                .isEqualTo(croqui.get("criadoEm"));
        assertThat(croquis.get("items").get(0).has("chave")).isFalse();
        assertThat(
                        chamar(get("/api/v1/meu-historico"), produtor, null, 200)
                                .get("items")
                                .get(0)
                                .get("totalPago")
                                .asText())
                .isEqualTo("100.00");
        chamar(
                get("/api/v1/relatorios/aplicacao-recursos")
                        .param("dataInicio", "2026-10-17")
                        .param("dataFim", "2026-01-01"),
                gestor,
                null,
                400);
    }

    @Test
    void rollbackDoBancoMantemDiarioEPermiteLimparUploadOrfao() {
        var chave = new AtomicReference<String>();
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        status -> {
                            chave.set(arquivos.guardar("teste".getBytes(), "text/plain"));
                            status.setRollbackOnly();
                        });
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM arquivo_pendente WHERE chave=?",
                                Long.class,
                                chave.get()))
                .isEqualTo(1);
        assertThat(arquivos.abrir(chave.get())).isEqualTo("teste".getBytes());
        jdbc.update(
                "UPDATE arquivo_pendente SET criado_em=? WHERE chave=?",
                Timestamp.from(Instant.now().minusSeconds(7200)),
                chave.get());
        limpeza.limpar();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM arquivo_pendente WHERE chave=?",
                                Long.class,
                                chave.get()))
                .isZero();
        assertThatThrownBy(() -> arquivos.abrir(chave.get())).isInstanceOf(FalhaApi.class);
    }

    @Test
    void limpezaPreservaArquivoReferenciadoEEdicaoDeCroquiAprovadoEhBloqueada() throws Exception {
        aporte("1000");
        long c = contrato();
        aprovar(c);
        long croqui = db.numero(db.buscar("contrato", c), "croqui_id");
        String chave = (String) db.buscar("croqui", croqui).get("chave");
        jdbc.update(
                "UPDATE arquivo_pendente SET criado_em=?",
                Timestamp.from(Instant.now().minusSeconds(7200)));
        limpeza.limpar();
        assertThat(arquivos.abrir(chave)).isNotEmpty();
        var upload =
                multipart(HttpMethod.PUT, "/api/v1/croquis/" + croqui)
                        .file(
                                new MockMultipartFile(
                                        "arquivo",
                                        "croqui.png",
                                        "image/png",
                                        arquivos.abrir(chave)))
                        .param("areaPsa", "1.0");
        chamar(upload, tecnico, null, 409);
    }

    @Test
    void administraContasSemCadastroPublicoNemRemocaoDoUltimoAdministrador() throws Exception {
        var nova =
                Map.of(
                        "nome",
                        "Novo técnico",
                        "email",
                        "novo@example.test",
                        "perfil",
                        "TECNICO",
                        "ativo",
                        true,
                        "senha",
                        "Senha-de-teste-123");
        long id = chamar(post("/api/v1/usuarios"), admin, nova, 201).get("id").asLong();
        chamar(post("/api/v1/usuarios"), admin, nova, 409);
        chamar(
                put("/api/v1/usuarios/" + id + "/senha"),
                admin,
                Map.of("novaSenha", "Outra-senha-teste-123"),
                204);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT versao_credenciais FROM usuario WHERE id=?",
                                Long.class,
                                id))
                .isEqualTo(1);
        long adminId = ((UsuarioSessao) admin.getPrincipal()).id();
        chamar(
                put("/api/v1/usuarios/" + adminId),
                admin,
                Map.of(
                        "nome",
                        "Admin",
                        "email",
                        "admin@example.test",
                        "perfil",
                        "ADMINISTRADOR",
                        "ativo",
                        false),
                409);
        chamar(post("/api/v1/usuarios"), tecnico, nova, 403);
    }

    @Test
    void naoExpoePagamentosNemArquivosDeOutroProdutor() throws Exception {
        aporte("1000");
        long c = contrato(), co = aprovar(c);
        long p =
                chamar(
                                post("/api/v1/pagamentos"),
                                gestor,
                                pagamento(co, "100", "isolamento-00000001"),
                                201)
                        .get("id")
                        .asLong();
        long outro =
                chamar(
                                post("/api/v1/produtores"),
                                tecnico,
                                Map.of("nome", "Outro", "telefone", "51"),
                                201)
                        .get("id")
                        .asLong();
        var outraConta = conta("outro", Perfil.PRODUTOR, outro);
        chamar(get("/api/v1/contratos/" + c + "/documento"), outraConta, null, 404);
        chamar(get("/api/v1/pagamentos/" + p), outraConta, null, 404);
        chamar(get("/api/v1/propriedades/" + propriedade + "/croquis"), outraConta, null, 404);
        assertThat(
                        chamar(get("/api/v1/meu-historico/pagamentos"), outraConta, null, 200)
                                .get("totalItems")
                                .asLong())
                .isZero();
    }

    @Test
    void parametrosPendentesNaoInventamPontuacaoEPermitemEditarValorDaTerra() throws Exception {
        chamar(
                put("/api/v1/projetos/" + projeto + "/parametros"),
                admin,
                Map.of(
                        "valorHectare",
                        "30000.00",
                        "percentual",
                        "0.04",
                        "metodologia",
                        "PENDENTE",
                        "grupos",
                        List.of()),
                200);
        long d = diagnostico();
        chamar(post("/api/v1/diagnosticos/" + d + "/concluir"), tecnico, null, 422);
        assertThat(
                        chamar(get("/api/v1/diagnosticos/" + d), tecnico, null, 200)
                                .get("pontuacao")
                                .isNull())
                .isTrue();
    }

    @Test
    void administradorConfiguraAntecedenciaPersistidaComValidacaoEAuditoria() throws Exception {
        String rota = "/api/v1/configuracoes/vencimento-contratos";
        assertThat(chamar(get(rota), admin, null, 200).get("antecedenciaDias").asInt())
                .isEqualTo(30);
        chamar(put(rota), admin, Map.of("antecedenciaDias", 15), 200);
        assertThat(chamar(get(rota), admin, null, 200).get("antecedenciaDias").asInt())
                .isEqualTo(15);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT antecedencia_dias FROM configuracao_vencimento WHERE id=1",
                                Integer.class))
                .isEqualTo(15);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM auditoria_evento WHERE recurso='configuracao_vencimento' AND operacao='ALTERAR'",
                                Integer.class))
                .isEqualTo(1);
        for (int invalido : List.of(-1, 3651))
            chamar(put(rota), admin, Map.of("antecedenciaDias", invalido), 422);
        chamar(put(rota), admin, Map.of(), 422);
        chamar(put(rota), admin, Map.of("antecedenciaDias", new BigDecimal("1.5")), 422);
        chamar(put(rota), gestor, Map.of("antecedenciaDias", 20), 403);
        mvc.perform(
                        put(rota)
                                .with(authentication(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"antecedenciaDias\":20}"))
                .andExpect(status().isForbidden());
        assertThat(chamar(get(rota), admin, null, 200).get("antecedenciaDias").asInt())
                .isEqualTo(15);
        chamar(put(rota), admin, Map.of("antecedenciaDias", 0), 200);
    }

    @Test
    void alertasFiltramAprovadosComLimitesPaginacaoEAtualizacaoImediata() throws Exception {
        var hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        var ids = new ArrayList<Long>();
        aporte("10000");
        for (int dias : List.of(-1, 0, 1, 30, 31)) {
            long id = contrato();
            aprovar(id);
            ids.add(id);
            // Datas controladas depois da aprovação isolam a consulta de alertas das regras de
            // contratação.
            jdbc.update(
                    "UPDATE contrato SET inicio=?,fim=? WHERE id=?",
                    hoje.minusYears(1),
                    hoje.plusDays(dias),
                    id);
        }
        long rascunho = contrato();
        jdbc.update("UPDATE contrato SET fim=? WHERE id=?", hoje, rascunho);
        String[] situacoes = {
            "VENCIDO",
            "VENCE_HOJE",
            "PROXIMO_DO_VENCIMENTO",
            "PROXIMO_DO_VENCIMENTO",
            "DENTRO_DO_PRAZO"
        };
        for (int i = 0; i < ids.size(); i++) {
            var detalhe = chamar(get("/api/v1/contratos/" + ids.get(i)), gestor, null, 200);
            assertThat(detalhe.get("situacaoVencimento").asText()).isEqualTo(situacoes[i]);
            assertThat(detalhe.get("diasParaVencimento").asLong())
                    .isEqualTo(List.of(-1L, 0L, 1L, 30L, 31L).get(i));
        }
        assertThat(
                        chamar(get("/api/v1/contratos/" + rascunho), gestor, null, 200)
                                .get("situacaoVencimento")
                                .asText())
                .isEqualTo("NAO_APLICAVEL");
        for (String situacao :
                List.of("VENCIDO", "VENCE_HOJE", "PROXIMO_DO_VENCIMENTO", "DENTRO_DO_PRAZO")) {
            var pagina =
                    chamar(
                            get("/api/v1/contratos")
                                    .param("situacaoVencimento", situacao)
                                    .param("size", "1"),
                            tecnico,
                            null,
                            200);
            assertThat(pagina.get("totalItems").asLong())
                    .isEqualTo(situacao.equals("PROXIMO_DO_VENCIMENTO") ? 2 : 1);
            assertThat(pagina.at("/items/0/situacaoVencimento").asText()).isEqualTo(situacao);
            var historico =
                    chamar(
                            get("/api/v1/meu-historico").param("situacaoVencimento", situacao),
                            produtor,
                            null,
                            200);
            assertThat(historico.get("totalItems")).isEqualTo(pagina.get("totalItems"));
        }
        var pagina2 =
                chamar(
                        get("/api/v1/contratos")
                                .param("situacaoVencimento", "PROXIMO_DO_VENCIMENTO")
                                .param("size", "1")
                                .param("page", "1"),
                        gestor,
                        null,
                        200);
        assertThat(pagina2.at("/items/0/id").asLong()).isEqualTo(ids.get(2));
        assertThat(
                        chamar(
                                        get("/api/v1/contratos")
                                                .param("situacaoVencimento", "VENCE_HOJE")
                                                .param("status", "RASCUNHO"),
                                        gestor,
                                        null,
                                        200)
                                .get("totalItems")
                                .asLong())
                .isZero();
        assertThat(
                        chamar(
                                        get("/api/v1/contratos")
                                                .param(
                                                        "situacaoVencimento",
                                                        "PROXIMO_DO_VENCIMENTO")
                                                .param("vencimentoAte", hoje.plusDays(1).toString())
                                                .param("projetoId", Long.toString(projeto)),
                                        gestor,
                                        null,
                                        200)
                                .get("totalItems")
                                .asLong())
                .isEqualTo(1);
        chamar(get("/api/v1/contratos").param("situacaoVencimento", "INVALIDO"), gestor, null, 400);
        chamar(
                get("/api/v1/meu-historico").param("situacaoVencimento", "INVALIDO"),
                produtor,
                null,
                400);
        chamar(
                put("/api/v1/configuracoes/vencimento-contratos"),
                admin,
                Map.of("antecedenciaDias", 0),
                200);
        assertThat(
                        chamar(
                                        get("/api/v1/contratos")
                                                .param(
                                                        "situacaoVencimento",
                                                        "PROXIMO_DO_VENCIMENTO"),
                                        gestor,
                                        null,
                                        200)
                                .get("totalItems")
                                .asLong())
                .isZero();
        assertThat(
                        chamar(get("/api/v1/contratos/" + ids.get(2)), produtor, null, 200)
                                .get("situacaoVencimento")
                                .asText())
                .isEqualTo("DENTRO_DO_PRAZO");
        long outroProdutor =
                chamar(
                                post("/api/v1/produtores"),
                                tecnico,
                                Map.of("nome", "Outro", "telefone", "51"),
                                201)
                        .get("id")
                        .asLong();
        var outraConta = conta("outroalerta", Perfil.PRODUTOR, outroProdutor);
        assertThat(
                        chamar(
                                        get("/api/v1/meu-historico")
                                                .param("situacaoVencimento", "VENCIDO")
                                                .param("produtorId", Long.toString(produtorId)),
                                        outraConta,
                                        null,
                                        200)
                                .get("totalItems")
                                .asLong())
                .isZero();
        chamar(get("/api/v1/contratos/" + ids.getFirst()), outraConta, null, 404);
    }

    @Test
    void rejeitaRespostaNulaSemApagarRespostasValidas() throws Exception {
        long diagnostico = diagnostico();
        chamar(
                put("/api/v1/diagnosticos/" + diagnostico + "/respostas"),
                tecnico,
                Map.of("respostas", List.of(Map.of("perguntaId", "p1", "alternativaId", "a1"))),
                200);
        chamar(
                put("/api/v1/diagnosticos/" + diagnostico + "/respostas"),
                tecnico,
                Map.of("respostas", Collections.singletonList(null)),
                422);
        var concluido =
                chamar(
                        post("/api/v1/diagnosticos/" + diagnostico + "/concluir"),
                        tecnico,
                        null,
                        200);
        assertThat(concluido.get("pontuacao").asText()).isEqualTo("1.000000");
    }

    @Test
    void gruposPreservamOrdemVersaoRespostasEPontuacao() throws Exception {
        var agua = new HashMap<>(pergunta("agua1", "0.75"));
        agua.put("alternativas", List.of(Map.of("id", "a1", "texto", "Parcial", "valor", "0.5")));
        var grupos = List.of(grupo("solo", pergunta("solo1", "0.25")), grupo("agua", agua));
        var configurado =
                chamar(
                        put("/api/v1/projetos/" + projeto + "/parametros"),
                        admin,
                        parametrosAgrupados(grupos),
                        200);
        long d = diagnostico();
        configurar("30000.00");
        var questionario =
                chamar(get("/api/v1/diagnosticos/" + d + "/questionario"), tecnico, null, 200);
        assertThat(questionario.get("parametroId").asLong())
                .isEqualTo(configurado.get("id").asLong());
        assertThat(questionario.at("/configuracao/grupos/0/id").asText()).isEqualTo("solo");
        assertThat(questionario.at("/configuracao/grupos/1/perguntas/0/id").asText())
                .isEqualTo("agua1");
        var respostas =
                List.of(
                        Map.of("perguntaId", "solo1", "alternativaId", "a1"),
                        Map.of("perguntaId", "agua1", "alternativaId", "a1"));
        chamar(
                put("/api/v1/diagnosticos/" + d + "/respostas"),
                tecnico,
                Map.of("respostas", respostas.subList(0, 1)),
                200);
        chamar(post("/api/v1/diagnosticos/" + d + "/concluir"), tecnico, null, 422);
        chamar(
                put("/api/v1/diagnosticos/" + d + "/respostas"),
                tecnico,
                Map.of("respostas", List.of(Map.of("perguntaId", "p1", "alternativaId", "a1"))),
                422);
        chamar(
                put("/api/v1/diagnosticos/" + d + "/respostas"),
                tecnico,
                Map.of("respostas", respostas),
                200);
        var concluido = chamar(post("/api/v1/diagnosticos/" + d + "/concluir"), tecnico, null, 200);
        assertThat(concluido.get("pontuacao").asText()).isEqualTo("0.625000");
        assertThat(json.readTree(concluido.get("memoriaJson").asText()).get("grupos"))
                .isEqualTo(questionario.at("/configuracao/grupos"));
        chamar(get("/api/v1/diagnosticos/" + d + "/questionario"), gestor, null, 200);
        chamar(get("/api/v1/diagnosticos/" + d + "/questionario"), produtor, null, 403);
        chamar(
                put("/api/v1/projetos/" + projeto + "/parametros"),
                tecnico,
                parametrosAgrupados(grupos),
                403);
    }

    @Test
    void rejeitaGruposInvalidosPerguntasDuplicadasEPesosIncorretos() throws Exception {
        var rota = "/api/v1/projetos/" + projeto + "/parametros";
        chamar(
                put(rota),
                admin,
                parametrosAgrupados(
                        List.of(
                                grupo("solo", pergunta("p1", "0.5")),
                                grupo("solo", pergunta("p2", "0.5")))),
                422);
        chamar(
                put(rota),
                admin,
                parametrosAgrupados(
                        List.of(
                                grupo("solo", pergunta("p1", "0.5")),
                                grupo("agua", pergunta("p1", "0.5")))),
                422);
        chamar(
                put(rota),
                admin,
                parametrosAgrupados(List.of(grupo("solo", pergunta("p1", "0.5")))),
                422);
        chamar(put(rota), admin, parametrosAgrupados(List.of(grupo("solo"))), 422);
        chamar(put(rota), admin, parametrosAgrupados(List.of(grupo("", pergunta("p1", "1")))), 422);
        chamar(
                put(rota),
                admin,
                parametrosAgrupados(
                        List.of(
                                Map.of(
                                        "id",
                                        "solo",
                                        "nome",
                                        " ",
                                        "perguntas",
                                        List.of(pergunta("p1", "1"))))),
                422);
        chamar(put(rota), admin, parametrosAgrupados(Arrays.asList((Object) null)), 422);
        chamar(
                put(rota),
                admin,
                parametrosAgrupados(
                        List.of(
                                Map.of(
                                        "id",
                                        "solo",
                                        "nome",
                                        "Solo",
                                        "perguntas",
                                        Arrays.asList((Object) null)))),
                422);
        chamar(
                put(rota),
                admin,
                parametrosAgrupados(List.of(grupo("LEGADO_SEM_GRUPO", pergunta("p1", "1")))),
                422);
        var semGrupo = new HashMap<>(parametrosAgrupados(List.of()));
        semGrupo.remove("grupos");
        semGrupo.put("perguntas", List.of(pergunta("p1", "1")));
        chamar(put(rota), admin, semGrupo, 422);
        var muitas = new ArrayList<Object>();
        for (int i = 0; i < 100; i++) muitas.add(pergunta("p" + i, "0.01"));
        chamar(
                put(rota),
                admin,
                parametrosAgrupados(
                        List.of(
                                grupo("solo", muitas.toArray()),
                                grupo("agua", pergunta("extra", "0.01")))),
                422);
    }

    @Test
    void questionarioHistoricoSemGruposContinuaLegivelSemReescreverVersao() throws Exception {
        String legado =
                json.writeValueAsString(
                        Map.of(
                                "valorHectare",
                                "20000.00",
                                "percentual",
                                "0.04",
                                "metodologia",
                                "DEMONSTRACAO_SOMA_PONDERADA",
                                "perguntas",
                                List.of(pergunta("p1", "1.000000"))));
        jdbc.update(
                "UPDATE parametro_projeto SET configuracao_json=? WHERE projeto_id=?",
                legado,
                projeto);
        long d = diagnostico();
        configurar("30000.00");
        var questionario =
                chamar(get("/api/v1/diagnosticos/" + d + "/questionario"), tecnico, null, 200);
        assertThat(questionario.at("/configuracao/grupos/0/id").asText())
                .isEqualTo("LEGADO_SEM_GRUPO");
        chamar(
                put("/api/v1/diagnosticos/" + d + "/respostas"),
                tecnico,
                Map.of("respostas", List.of(Map.of("perguntaId", "p1", "alternativaId", "a1"))),
                200);
        assertThat(
                        chamar(post("/api/v1/diagnosticos/" + d + "/concluir"), tecnico, null, 200)
                                .get("pontuacao")
                                .asText())
                .isEqualTo("1.000000");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT configuracao_json FROM parametro_projeto WHERE id=?",
                                String.class,
                                questionario.get("parametroId").asLong()))
                .isEqualTo(legado);
    }

    @Test
    void vigenciaRejeitaFimAnteriorEAceitaMesmoDia() throws Exception {
        long calc = calculo();
        var inicio = LocalDate.of(2026, 10, 18);
        var entrada =
                new HashMap<String, Object>(
                        Map.of(
                                "projetoId",
                                projeto,
                                "calculoId",
                                calc,
                                "inicio",
                                inicio.toString(),
                                "fim",
                                inicio.minusDays(1).toString()));
        assertThat(chamar(post("/api/v1/contratos"), gestor, entrada, 422).get("codigo").asText())
                .isEqualTo("VIGENCIA_INVALIDA");
        entrada.put("fim", inicio.toString());
        var contrato = chamar(post("/api/v1/contratos"), gestor, entrada, 201);
        assertThat(contrato.get("inicio").asText()).isEqualTo(inicio.toString());
        assertThat(contrato.get("fim").asText()).isEqualTo(inicio.toString());
        entrada.put("fim", inicio.minusDays(1).toString());
        chamar(put("/api/v1/contratos/" + contrato.get("id").asLong()), gestor, entrada, 422);
        assertThat(
                        chamar(
                                        get("/api/v1/contratos/" + contrato.get("id").asLong()),
                                        gestor,
                                        null,
                                        200)
                                .get("fim")
                                .asText())
                .isEqualTo(inicio.toString());
    }

    @Test
    void filtroVencimentoIncluiLimiteEVencidosMasExcluiDatasPosteriores() throws Exception {
        var limite = LocalDate.of(2026, 11, 30);
        var ids = new ArrayList<Long>();
        for (var fim : List.of(limite.minusDays(1), limite, limite.plusDays(1))) {
            long calc = calculo();
            ids.add(
                    chamar(
                                    post("/api/v1/contratos"),
                                    gestor,
                                    Map.of(
                                            "projetoId",
                                            projeto,
                                            "calculoId",
                                            calc,
                                            "inicio",
                                            "2026-01-01",
                                            "fim",
                                            fim.toString()),
                                    201)
                            .get("id")
                            .asLong());
        }
        var pagina =
                chamar(
                        get("/api/v1/contratos")
                                .param("projetoId", Long.toString(projeto))
                                .param("status", "RASCUNHO")
                                .param("vencimentoAte", limite.toString()),
                        gestor,
                        null,
                        200);
        assertThat(pagina.get("totalItems").asInt()).isEqualTo(2);
        var encontrados = new ArrayList<Long>();
        pagina.get("items").forEach(c -> encontrados.add(c.get("id").asLong()));
        assertThat(encontrados).containsExactlyInAnyOrder(ids.get(0), ids.get(1));
        assertThat(
                        chamar(
                                        get("/api/v1/contratos")
                                                .param("vencimentoAte", limite.toString())
                                                .param("status", "APROVADO"),
                                        gestor,
                                        null,
                                        200)
                                .get("totalItems")
                                .asInt())
                .isZero();
        chamar(
                get("/api/v1/contratos").param("vencimentoAte", limite.toString()),
                produtor,
                null,
                403);
        chamar(get("/api/v1/contratos").param("vencimentoAte", "data-invalida"), gestor, null, 400);
    }

    @Test
    void assinaturaSimuladaExigeDocumentoImpedeDuplicidadeERestringePerfil() throws Exception {
        long calc = calculo();
        long rascunho =
                chamar(
                                post("/api/v1/contratos"),
                                gestor,
                                Map.of(
                                        "projetoId",
                                        projeto,
                                        "calculoId",
                                        calc,
                                        "inicio",
                                        LocalDate.now().toString(),
                                        "fim",
                                        LocalDate.now().plusMonths(1).toString()),
                                201)
                        .get("id")
                        .asLong();
        var registro = Map.of("dataRegistro", LocalDate.now().toString());
        chamar(
                post("/api/v1/contratos/" + rascunho + "/assinatura-simulada"),
                gestor,
                registro,
                409);
        long id = contrato();
        chamar(post("/api/v1/contratos/" + id + "/assinatura-simulada"), tecnico, registro, 403);
        chamar(
                post("/api/v1/contratos/" + id + "/assinatura-simulada"),
                gestor,
                Map.of("dataRegistro", LocalDate.now().plusDays(1).toString()),
                422);
        var assinado =
                chamar(
                        post("/api/v1/contratos/" + id + "/assinatura-simulada"),
                        gestor,
                        registro,
                        200);
        assertThat(assinado.get("assinaturaEm").isNull()).isFalse();
        chamar(post("/api/v1/contratos/" + id + "/assinatura-simulada"), gestor, registro, 409);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM auditoria_evento WHERE operacao='ASSINATURA_SIMULADA' AND recurso='contrato' AND recurso_id=?",
                                Long.class,
                                id))
                .isEqualTo(1);
    }

    @Test
    void medeApuracaoECalculoLocalComCemPerguntas() throws Exception {
        var grupos = new ArrayList<Object>();
        var respostas = new ArrayList<Object>();
        for (int g = 0; g < 4; g++) {
            var perguntas = new ArrayList<Object>();
            for (int q = 0; q < 25; q++) {
                String id = "g" + g + "p" + q;
                perguntas.add(pergunta(id, "0.01"));
                respostas.add(Map.of("perguntaId", id, "alternativaId", "a1"));
            }
            grupos.add(grupo("grupo" + g, perguntas.toArray()));
        }
        chamar(
                put("/api/v1/projetos/" + projeto + "/parametros"),
                admin,
                parametrosAgrupados(grupos),
                200);
        var amostras = new ArrayList<Map<String, Object>>();
        for (int n = 0; n < 12; n++) {
            long id = diagnostico();
            chamar(
                    put("/api/v1/diagnosticos/" + id + "/respostas"),
                    tecnico,
                    Map.of("respostas", respostas),
                    200);
            long inicio = System.nanoTime();
            var apurado =
                    chamar(post("/api/v1/diagnosticos/" + id + "/concluir"), tecnico, null, 200);
            long meio = System.nanoTime();
            long calc =
                    chamar(
                                    post("/api/v1/calculos"),
                                    tecnico,
                                    Map.of("diagnosticoId", id, "areaContratada", "1.0000"),
                                    201)
                            .get("id")
                            .asLong();
            var valor = chamar(get("/api/v1/calculos/" + calc), tecnico, null, 200);
            long fim = System.nanoTime();
            assertThat(apurado.get("pontuacao").asText()).isEqualTo("1.000000");
            assertThat(valor.get("valorAnual").asText()).isEqualTo("800.00");
            if (n >= 2)
                amostras.add(
                        Map.of(
                                "apuracaoMs",
                                (meio - inicio) / 1_000_000.0,
                                "calculoEConsultaMs",
                                (fim - meio) / 1_000_000.0,
                                "totalMs",
                                (fim - inicio) / 1_000_000.0));
        }
        var saida = Path.of("target", "aceitacao", "desempenho-local.json");
        Files.createDirectories(saida.getParent());
        Files.writeString(
                saida,
                json.writeValueAsString(
                        Map.of(
                                "executadoEm",
                                Instant.now().toString(),
                                "ambiente",
                                "MockMvc com MariaDB descartável; sem rede HTTP, navegador ou VPS",
                                "metodologia",
                                "DEMONSTRACAO_SOMA_PONDERADA",
                                "perguntas",
                                100,
                                "aquecimentos",
                                2,
                                "limiteMs",
                                5000,
                                "amostras",
                                amostras)));
        assertThat(amostras)
                .allSatisfy(a -> assertThat((Double) a.get("totalMs")).isLessThanOrEqualTo(5000.0));
    }

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping rotasMvc;

    Object corpoPermissao(String tipo) {
        long ausente = 999999999L;
        return switch (tipo) {
            case "usuario" ->
                    Map.of(
                            "nome",
                            "Conta teste",
                            "email",
                            "rota@example.test",
                            "perfil",
                            "TECNICO",
                            "ativo",
                            true,
                            "senha",
                            "Senha-de-teste-123");
            case "vencimento" -> Map.of("antecedenciaDias", 30);
            case "senha" -> Map.of("novaSenha", "Senha-de-teste-123");
            case "produtor" -> Map.of("nome", "Cadastro de teste", "telefone", "51");
            case "propriedade" ->
                    Map.of(
                            "nome",
                            "Teste",
                            "produtorId",
                            ausente,
                            "localidadeId",
                            ausente,
                            "areaTotal",
                            "1",
                            "carSituacao",
                            "NAO_POSSUI");
            case "fonte" -> Map.of("tipo", "NASCENTE", "protecao", "PROTEGIDA", "ativo", true);
            case "levantamento" -> Map.of("situacao", "AUSENTES");
            case "visita" ->
                    Map.of("dataVisita", LocalDate.now().toString(), "observacoes", "Teste");
            case "parametros" ->
                    Map.of(
                            "valorHectare",
                            "20000",
                            "percentual",
                            "0.04",
                            "metodologia",
                            "PENDENTE",
                            "grupos",
                            List.of());
            case "diagnostico" ->
                    Map.of("projetoId", ausente, "dataVisita", LocalDate.now().toString());
            case "respostas" -> Map.of("respostas", List.of());
            case "correcao" -> Map.of("motivo", "Correção de teste");
            case "calculo" -> Map.of("diagnosticoId", ausente, "areaContratada", "1");
            case "contrato" ->
                    Map.of(
                            "projetoId",
                            ausente,
                            "calculoId",
                            ausente,
                            "inicio",
                            LocalDate.now().toString(),
                            "fim",
                            LocalDate.now().toString());
            case "assinatura" -> Map.of("dataRegistro", LocalDate.now().toString());
            case "aprovacao" -> Map.of("exercicio", LocalDate.now().getYear());
            case "aporte" ->
                    Map.of(
                            "tipo",
                            "APORTE",
                            "valor",
                            "1",
                            "dataAporte",
                            LocalDate.now().toString(),
                            "descricao",
                            "Teste");
            case "pagamento" ->
                    Map.of(
                            "compromissoId",
                            ausente,
                            "projetoId",
                            ausente,
                            "valor",
                            "1",
                            "dataPagamento",
                            LocalDate.now().toString(),
                            "referencia",
                            "Teste",
                            "chaveOperacao",
                            "teste-matriz-0001");
            default -> null;
        };
    }

    @Test
    void matrizDeTodasAsRotasValidaPerfisERecusaAnonimos() throws Exception {
        var matriz =
                json.readTree(
                        Files.readString(Path.of("src/test/resources/permissoes-rotas.json")));
        var previstas = new TreeSet<String>();
        matriz.forEach(r -> previstas.add(r.get("metodo").asText() + " " + r.get("rota").asText()));
        var registradas = new TreeSet<String>();
        rotasMvc.getHandlerMethods()
                .forEach(
                        (mapping, handler) -> {
                            if (handler.getBeanType()
                                            .getPackageName()
                                            .startsWith("br.unisc.psagestor")
                                    && !handler.getBeanType()
                                            .getSimpleName()
                                            .equals("AuthController"))
                                mapping.getMethodsCondition()
                                        .getMethods()
                                        .forEach(
                                                m ->
                                                        mapping.getPatternValues()
                                                                .forEach(
                                                                        path ->
                                                                                registradas.add(
                                                                                        m.name()
                                                                                                + " "
                                                                                                + path)));
                        });
        assertThat(previstas).containsExactlyElementsOf(registradas);
        var perfis = Map.of("A", admin, "G", gestor, "T", tecnico, "P", produtor);
        int verificacoes = 0;
        for (var rota : matriz)
            for (String perfil : List.of("ANONIMO", "A", "G", "T", "P")) {
                var metodo = HttpMethod.valueOf(rota.get("metodo").asText());
                String url = rota.get("rota").asText().replace("{id}", "999999999");
                AbstractMockHttpServletRequestBuilder<?> request;
                if (rota.get("corpo").asText().equals("arquivo")) {
                    var imagem = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
                    var bytes = new ByteArrayOutputStream();
                    ImageIO.write(imagem, "png", bytes);
                    request =
                            multipart(metodo, url)
                                    .file(
                                            new MockMultipartFile(
                                                    "arquivo",
                                                    "teste.png",
                                                    "image/png",
                                                    bytes.toByteArray()))
                                    .param("areaPsa", "1");
                } else {
                    request = request(metodo, url);
                    var body = corpoPermissao(rota.get("corpo").asText());
                    if (body != null)
                        request.contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(body));
                }
                request.with(csrf());
                if (!perfil.equals("ANONIMO")) request.with(authentication(perfis.get(perfil)));
                var resposta = mvc.perform(request).andReturn();
                int status = resposta.getResponse().getStatus();
                String contexto =
                        metodo
                                + " "
                                + url
                                + " perfil="
                                + perfil
                                + " resposta="
                                + resposta.getResponse().getContentAsString();
                if (perfil.equals("ANONIMO")) assertThat(status).as(contexto).isEqualTo(401);
                else if (!rota.get("perfis").asText().contains(perfil))
                    assertThat(status).as(contexto).isEqualTo(403);
                else assertThat(status).as(contexto).isIn(200, 201, 204, 404, 409);
                verificacoes++;
            }
        assertThat(verificacoes).isEqualTo(previstas.size() * 5);
    }

    @Test
    void produtorSoConsultaSeusRecursosMesmoAlterandoIdsEFiltros() throws Exception {
        aporte("3000");
        long c1 = contrato(), co1 = aprovar(c1);
        long pagamento1 =
                chamar(
                                post("/api/v1/pagamentos"),
                                gestor,
                                pagamento(co1, "100", "isolamento-completo-01"),
                                201)
                        .get("id")
                        .asLong();
        long propriedade1 = propriedade, produtor1 = produtorId;
        var contrato1 = db.buscar("contrato", c1);
        long calc1 = Dados.numero(contrato1, "calculo_id"),
                croqui1 = Dados.numero(contrato1, "croqui_id");
        long diag1 = Dados.numero(db.buscar("calculo_psa", calc1), "diagnostico_id");
        produtorId =
                chamar(
                                post("/api/v1/produtores"),
                                tecnico,
                                Map.of(
                                        "nome",
                                        "Segundo produtor",
                                        "documento",
                                        "98765432100",
                                        "endereco",
                                        "Outro endereço",
                                        "telefone",
                                        "51"),
                                201)
                        .get("id")
                        .asLong();
        var segundo = conta("segundo", Perfil.PRODUTOR, produtorId);
        propriedade =
                chamar(
                                post("/api/v1/propriedades"),
                                tecnico,
                                Map.of(
                                        "nome",
                                        "Outra área",
                                        "produtorId",
                                        produtorId,
                                        "localidadeId",
                                        localidade,
                                        "areaTotal",
                                        "10",
                                        "carSituacao",
                                        "NAO_POSSUI"),
                                201)
                        .get("id")
                        .asLong();
        long c2 = contrato(), co2 = aprovar(c2);
        long pagamento2 =
                chamar(
                                post("/api/v1/pagamentos"),
                                gestor,
                                pagamento(co2, "200", "isolamento-completo-02"),
                                201)
                        .get("id")
                        .asLong();
        var contrato2 = db.buscar("contrato", c2);
        long calc2 = Dados.numero(contrato2, "calculo_id"),
                croqui2 = Dados.numero(contrato2, "croqui_id"),
                diag2 = Dados.numero(db.buscar("calculo_psa", calc2), "diagnostico_id");
        String[] primeiras = {
            "/produtores/" + produtor1,
            "/propriedades/" + propriedade1,
            "/propriedades/" + propriedade1 + "/fontes-hidricas",
            "/propriedades/" + propriedade1 + "/visitas",
            "/propriedades/" + propriedade1 + "/croquis",
            "/croquis/" + croqui1 + "/arquivo",
            "/contratos/" + c1,
            "/contratos/" + c1 + "/documento",
            "/pagamentos/" + pagamento1,
            "/propriedades/" + propriedade1 + "/diagnosticos",
            "/diagnosticos/" + diag1,
            "/calculos/" + calc1
        };
        String[] segundas = {
            "/produtores/" + produtorId,
            "/propriedades/" + propriedade,
            "/propriedades/" + propriedade + "/fontes-hidricas",
            "/propriedades/" + propriedade + "/visitas",
            "/propriedades/" + propriedade + "/croquis",
            "/croquis/" + croqui2 + "/arquivo",
            "/contratos/" + c2,
            "/contratos/" + c2 + "/documento",
            "/pagamentos/" + pagamento2,
            "/propriedades/" + propriedade + "/diagnosticos",
            "/diagnosticos/" + diag2,
            "/calculos/" + calc2
        };
        for (int i = 0; i < primeiras.length; i++) {
            mvc.perform(get("/api/v1" + primeiras[i]).with(authentication(produtor)))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/v1" + segundas[i]).with(authentication(segundo)))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/v1" + primeiras[i]).with(authentication(segundo)))
                    .andExpect(status().isNotFound());
            mvc.perform(get("/api/v1" + segundas[i]).with(authentication(produtor)))
                    .andExpect(status().isNotFound());
        }
        for (var auth : List.of(produtor, segundo)) {
            boolean primeiro = auth == produtor;
            var historico =
                    chamar(
                            get("/api/v1/meu-historico")
                                    .param(
                                            "produtorId",
                                            Long.toString(primeiro ? produtorId : produtor1)),
                            auth,
                            null,
                            200);
            assertThat(historico.get("totalItems").asInt()).isEqualTo(1);
            assertThat(historico.at("/items/0/id").asLong()).isEqualTo(primeiro ? c1 : c2);
            var extrato =
                    chamar(
                            get("/api/v1/meu-historico/pagamentos")
                                    .param(
                                            "produtorId",
                                            Long.toString(primeiro ? produtorId : produtor1)),
                            auth,
                            null,
                            200);
            assertThat(extrato.get("totalItems").asInt()).isEqualTo(1);
            assertThat(extrato.at("/items/0/id").asLong())
                    .isEqualTo(primeiro ? pagamento1 : pagamento2);
        }
    }

    record LinhaRelatorio(long id, long projeto, long subBacia, LocalDate data, BigDecimal valor) {}

    void conferirRelatorio(
            List<LinhaRelatorio> linhas,
            Long projetoFiltro,
            Long subFiltro,
            LocalDate inicio,
            LocalDate fim)
            throws Exception {
        var esperadas =
                linhas.stream()
                        .filter(l -> projetoFiltro == null || l.projeto() == projetoFiltro)
                        .filter(l -> subFiltro == null || l.subBacia() == subFiltro)
                        .filter(l -> inicio == null || !l.data().isBefore(inicio))
                        .filter(l -> fim == null || !l.data().isAfter(fim))
                        .toList();
        var total =
                esperadas.stream()
                        .map(LinhaRelatorio::valor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        var ids =
                esperadas.stream()
                        .map(LinhaRelatorio::id)
                        .sorted(Comparator.reverseOrder())
                        .toList();
        for (int pagina : List.of(0, 1, 99)) {
            var request =
                    get("/api/v1/relatorios/aplicacao-recursos")
                            .param("page", Integer.toString(pagina))
                            .param("size", "2");
            if (projetoFiltro != null) request.param("projetoId", projetoFiltro.toString());
            if (subFiltro != null) request.param("subBaciaId", subFiltro.toString());
            if (inicio != null) request.param("dataInicio", inicio.toString());
            if (fim != null) request.param("dataFim", fim.toString());
            var resposta = chamar(request, gestor, null, 200);
            assertThat(resposta.get("totalItems").asInt()).isEqualTo(esperadas.size());
            assertThat(new BigDecimal(resposta.get("totalPago").asText()))
                    .isEqualByComparingTo(total);
            assertThat(resposta.get("page").asInt()).isEqualTo(pagina);
            assertThat(resposta.get("size").asInt()).isEqualTo(2);
            var retornados = new ArrayList<Long>();
            resposta.get("items")
                    .forEach(
                            item -> {
                                retornados.add(item.get("id").asLong());
                                var origem =
                                        esperadas.stream()
                                                .filter(l -> l.id() == item.get("id").asLong())
                                                .findFirst()
                                                .orElseThrow();
                                assertThat(item.get("projetoId").asLong())
                                        .isEqualTo(origem.projeto());
                                assertThat(item.get("produtorId").asLong()).isEqualTo(produtorId);
                                assertThat(item.get("contratoId").asLong()).isPositive();
                                assertThat(item.get("dataPagamento").asText())
                                        .isEqualTo(origem.data().toString());
                                assertThat(new BigDecimal(item.get("valor").asText()))
                                        .isEqualByComparingTo(origem.valor());
                            });
            assertThat(retornados)
                    .containsExactlyElementsOf(ids.stream().skip(pagina * 2L).limit(2).toList());
        }
    }

    @Test
    void relatoriosFiltramPeriodoProjetoSubBaciaCombinacoesEPaginacaoSemAlterarTotais()
            throws Exception {
        long projeto1 = projeto;
        long sub1 = db.inserir("INSERT INTO sub_bacia(nome) VALUES ('Sub A')"),
                sub2 = db.inserir("INSERT INTO sub_bacia(nome) VALUES ('Sub B')");
        long projeto2 =
                db.inserir(
                        "INSERT INTO projeto(nome,origem_recursos,ativo) VALUES ('Projeto B','Teste',TRUE)");
        var dados = new ArrayList<LinhaRelatorio>();
        var data = LocalDate.now().minusDays(4);
        int sequencia = 0;
        for (int c = 0; c < 3; c++) {
            projeto = c == 2 ? projeto2 : projeto1;
            long sub = c == 1 ? sub2 : sub1;
            configurar("20000");
            if (c != 1) aporte("5000");
            propriedade =
                    chamar(
                                    post("/api/v1/propriedades"),
                                    tecnico,
                                    Map.of(
                                            "nome",
                                            "Área relatório " + c,
                                            "produtorId",
                                            produtorId,
                                            "localidadeId",
                                            localidade,
                                            "subBaciaId",
                                            sub,
                                            "areaTotal",
                                            "10",
                                            "carSituacao",
                                            "NAO_POSSUI"),
                                    201)
                            .get("id")
                            .asLong();
            long compromisso = aprovar(contrato());
            for (int dia = 0; dia < 4; dia++) {
                var valor = new BigDecimal("10.11").multiply(BigDecimal.valueOf(++sequencia));
                var entrada =
                        new HashMap<>(
                                pagamento(
                                        compromisso,
                                        valor.toPlainString(),
                                        "relatorio-filtros-" + sequencia));
                entrada.put("dataPagamento", data.plusDays(dia).toString());
                long id =
                        chamar(post("/api/v1/pagamentos"), gestor, entrada, 201).get("id").asLong();
                dados.add(new LinhaRelatorio(id, projeto, sub, data.plusDays(dia), valor));
            }
        }
        for (Long pf : Arrays.asList(null, projeto1, projeto2))
            for (Long sf : Arrays.asList(null, sub1, sub2))
                for (int periodo = 0; periodo < 4; periodo++)
                    conferirRelatorio(
                            dados,
                            pf,
                            sf,
                            (periodo & 1) != 0 ? data.plusDays(1) : null,
                            (periodo & 2) != 0 ? data.plusDays(2) : null);
        conferirRelatorio(dados, null, null, data.plusDays(1), data.plusDays(1));
        conferirRelatorio(dados, 999999999L, null, null, null);
        conferirRelatorio(dados, null, 999999999L, null, null);
        conferirRelatorio(dados, null, null, data.minusDays(10), data.minusDays(1));
        chamar(
                get("/api/v1/relatorios/aplicacao-recursos")
                        .param("dataInicio", data.plusDays(1).toString())
                        .param("dataFim", data.toString()),
                gestor,
                null,
                400);
        chamar(
                get("/api/v1/relatorios/aplicacao-recursos").param("dataInicio", "invalida"),
                gestor,
                null,
                400);
    }

    @Test
    void relatorioSemPagamentosRetornaListaVaziaETotalZero() throws Exception {
        conferirRelatorio(List.of(), null, null, null, null);
    }

    @Test
    void rotasDeAutenticacaoDocumentacaoEMetricasRespeitamPerfis() throws Exception {
        var autenticacao = new TreeSet<String>();
        rotasMvc.getHandlerMethods()
                .forEach(
                        (mapping, handler) -> {
                            if (handler.getBeanType().getSimpleName().equals("AuthController"))
                                mapping.getMethodsCondition()
                                        .getMethods()
                                        .forEach(
                                                m ->
                                                        mapping.getPatternValues()
                                                                .forEach(
                                                                        path ->
                                                                                autenticacao.add(
                                                                                        m.name()
                                                                                                + " "
                                                                                                + path)));
                        });
        assertThat(autenticacao)
                .containsExactlyInAnyOrder(
                        "GET /api/v1/auth/csrf",
                        "POST /api/v1/auth/login",
                        "GET /api/v1/auth/me",
                        "PUT /api/v1/auth/senha");
        mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(
                        put("/api/v1/auth/senha")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "senhaAtual",
                                                        "nao-utilizada",
                                                        "novaSenha",
                                                        "Senha-nova-teste-123"))))
                .andExpect(status().isUnauthorized());
        for (String rota :
                List.of(
                        "/swagger-ui/index.html",
                        "/v3/api-docs",
                        "/v3/api-docs/swagger-config",
                        "/actuator/metrics",
                        "/actuator/metrics/jvm.memory.used",
                        "/actuator/health",
                        "/actuator/health/readiness")) {
            mvc.perform(get(rota)).andExpect(status().isUnauthorized());
            mvc.perform(get(rota).with(authentication(admin))).andExpect(status().isOk());
            for (var auth : List.of(gestor, tecnico, produtor))
                mvc.perform(get(rota).with(authentication(auth))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        for (var auth : List.of(admin, gestor, tecnico, produtor)) {
            mvc.perform(get("/actuator/env").with(authentication(auth)))
                    .andExpect(status().isForbidden());
            var usuario = (UsuarioSessao) auth.getPrincipal();
            var login =
                    mvc.perform(
                                    post("/api/v1/auth/login")
                                            .with(csrf())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    json.writeValueAsString(
                                                            Map.of(
                                                                    "email",
                                                                    usuario.email(),
                                                                    "senha",
                                                                    "nao-utilizada"))))
                            .andExpect(status().isOk())
                            .andReturn();
            var session = (MockHttpSession) login.getRequest().getSession(false);
            mvc.perform(get("/api/v1/auth/me").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("perfil").value(usuario.perfil().name()));
            mvc.perform(post("/api/v1/auth/logout").session(session).with(csrf()))
                    .andExpect(status().isNoContent());
            assertThat(session.isInvalid()).isTrue();
            mvc.perform(
                            put("/api/v1/auth/senha")
                                    .with(authentication(auth))
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            json.writeValueAsString(
                                                    Map.of(
                                                            "senhaAtual",
                                                            "nao-utilizada",
                                                            "novaSenha",
                                                            "Senha-nova-teste-123"))))
                    .andExpect(status().isNoContent());
        }
    }

    Authentication conta(String nome, Perfil perfil, Long produtor) {
        var u =
                usuarios.saveAndFlush(
                        new Usuario(
                                nome,
                                nome + "@example.test",
                                "{noop}nao-utilizada",
                                perfil,
                                produtor,
                                null));
        return new UsernamePasswordAuthenticationToken(
                UsuarioSessao.de(u),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name())));
    }

    JsonNode chamar(
            AbstractMockHttpServletRequestBuilder<?> r,
            Authentication auth,
            Object body,
            int status)
            throws Exception {
        r.with(authentication(auth)).with(csrf());
        if (body != null)
            r.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var result = mvc.perform(r).andReturn();
        if (result.getResponse().getStatus() != status)
            throw new AssertionError(
                    "HTTP esperado "
                            + status
                            + " recebido "
                            + result.getResponse().getStatus()
                            + " em "
                            + result.getRequest().getRequestURI(),
                    result.getResolvedException());
        String response = result.getResponse().getContentAsString();
        return response.isBlank() ? json.nullNode() : json.readTree(response);
    }

    Map<String, Object> pergunta(String id, String peso) {
        return Map.of(
                "id",
                id,
                "texto",
                "Pergunta demonstrativa",
                "obrigatoria",
                true,
                "peso",
                peso,
                "alternativas",
                List.of(Map.of("id", "a1", "texto", "Sim", "valor", "1.000000")));
    }

    Map<String, Object> grupo(String id, Object... perguntas) {
        return Map.of("id", id, "nome", id, "perguntas", List.of(perguntas));
    }

    Map<String, Object> parametrosAgrupados(List<?> grupos) {
        return Map.of(
                "valorHectare",
                "20000.00",
                "percentual",
                "0.04",
                "metodologia",
                "DEMONSTRACAO_SOMA_PONDERADA",
                "grupos",
                grupos);
    }

    void configurar(String valor) throws Exception {
        var body =
                new HashMap<>(
                        parametrosAgrupados(List.of(grupo("solo", pergunta("p1", "1.000000")))));
        body.put("valorHectare", valor);
        chamar(put("/api/v1/projetos/" + projeto + "/parametros"), admin, body, 200);
    }

    long diagnostico() throws Exception {
        return chamar(
                        post("/api/v1/propriedades/" + propriedade + "/diagnosticos"),
                        tecnico,
                        Map.of("projetoId", projeto, "dataVisita", LocalDate.now().toString()),
                        201)
                .get("id")
                .asLong();
    }

    long calculo() throws Exception {
        long d = diagnostico();
        chamar(
                put("/api/v1/diagnosticos/" + d + "/respostas"),
                tecnico,
                Map.of("respostas", List.of(Map.of("perguntaId", "p1", "alternativaId", "a1"))),
                200);
        chamar(post("/api/v1/diagnosticos/" + d + "/concluir"), tecnico, null, 200);
        return chamar(
                        post("/api/v1/calculos"),
                        gestor,
                        Map.of("diagnosticoId", d, "areaContratada", "1.0000"),
                        201)
                .get("id")
                .asLong();
    }

    long contrato() throws Exception {
        long calc = calculo();
        var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        var out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        var upload =
                multipart("/api/v1/propriedades/" + propriedade + "/croquis")
                        .file(
                                new MockMultipartFile(
                                        "arquivo", "croqui.png", "image/png", out.toByteArray()))
                        .param("areaPsa", "1.0000");
        long croqui = chamar(upload, tecnico, null, 201).get("id").asLong();
        long id =
                chamar(
                                post("/api/v1/contratos"),
                                gestor,
                                Map.of(
                                        "projetoId",
                                        projeto,
                                        "calculoId",
                                        calc,
                                        "croquiId",
                                        croqui,
                                        "inicio",
                                        LocalDate.now().toString(),
                                        "fim",
                                        LocalDate.now().plusMonths(6).toString()),
                                201)
                        .get("id")
                        .asLong();
        chamar(post("/api/v1/contratos/" + id + "/gerar-documento"), gestor, null, 200);
        return id;
    }

    void aporte(String valor) throws Exception {
        chamar(
                post("/api/v1/projetos/" + projeto + "/aportes"),
                gestor,
                Map.of(
                        "tipo",
                        "SALDO_INICIAL",
                        "valor",
                        valor,
                        "dataAporte",
                        LocalDate.now().toString(),
                        "descricao",
                        "Saldo de teste"),
                201);
    }

    long aprovar(long id) throws Exception {
        chamar(
                post("/api/v1/contratos/" + id + "/aprovar"),
                gestor,
                Map.of("exercicio", LocalDate.now().getYear()),
                200);
        return chamar(get("/api/v1/contratos/" + id + "/compromissos"), gestor, null, 200)
                .get("items")
                .get(0)
                .get("id")
                .asLong();
    }

    Map<String, Object> pagamento(long compromisso, String valor, String chave) {
        return Map.of(
                "compromissoId",
                compromisso,
                "projetoId",
                projeto,
                "valor",
                valor,
                "dataPagamento",
                LocalDate.now().toString(),
                "referencia",
                "Simulação",
                "chaveOperacao",
                chave);
    }

    JsonNode consultarSaldo(Long filtro) throws Exception {
        var request = get("/api/v1/financeiro/saldo");
        if (filtro != null) request.param("projetoId", filtro.toString());
        return chamar(request, gestor, null, 200);
    }

    void verificarOcorrencia(JsonNode saldo, String codigo, String recurso, long id) {
        assertThat(saldo.get("consolidacaoCompleta").asBoolean()).isFalse();
        assertThat(saldo.get("statusConsolidacao").asText()).isEqualTo("INCOMPLETA");
        assertThat(saldo.get("mensagem").asText()).contains("incompleta", "indicativos");
        boolean encontrou = false;
        for (var item : saldo.get("inconsistencias")) {
            if (item.get("codigo").asText().equals(codigo)
                    && item.get("recurso").asText().equals(recurso)
                    && item.get("registroId").asLong() == id) encontrou = true;
        }
        assertThat(encontrou).as("Ocorrência %s em %s %s", codigo, recurso, id).isTrue();
        assertThat(saldo.get("quantidadeInconsistencias").asInt())
                .isEqualTo(saldo.get("inconsistencias").size());
    }

    @Test
    void saldoSemMovimentosEhCompletoEZerado() throws Exception {
        for (Long filtro : Arrays.asList(null, projeto)) {
            var saldo = consultarSaldo(filtro);
            assertThat(saldo.get("consolidacaoCompleta").asBoolean()).isTrue();
            assertThat(saldo.get("statusConsolidacao").asText()).isEqualTo("COMPLETA");
            assertThat(saldo.get("inconsistencias").size()).isZero();
            for (String campo :
                    List.of(
                            "recursos",
                            "totalPago",
                            "saldoCaixa",
                            "saldoComprometido",
                            "saldoDisponivel"))
                assertThat(new BigDecimal(saldo.get(campo).asText())).isEqualByComparingTo("0");
        }
        chamar(
                get("/api/v1/financeiro/saldo").param("projetoId", "9223372036854775807"),
                gestor,
                null,
                404);
        chamar(get("/api/v1/financeiro/saldo"), tecnico, null, 403);
        chamar(get("/api/v1/financeiro/saldo"), admin, null, 403);
    }

    @Test
    void saldoIdentificaTodosOsPagamentosAcimaDoCompromissoSemAlterarDados() throws Exception {
        aporte("2000");
        long c = contrato(), co = aprovar(c);
        long p1 =
                chamar(
                                post("/api/v1/pagamentos"),
                                gestor,
                                pagamento(co, "300", "conferencia-000001"),
                                201)
                        .get("id")
                        .asLong();
        long p2 =
                chamar(
                                post("/api/v1/pagamentos"),
                                gestor,
                                pagamento(co, "400", "conferencia-000002"),
                                201)
                        .get("id")
                        .asLong();
        assertThat(consultarSaldo(projeto).get("consolidacaoCompleta").asBoolean()).isTrue();
        // Corrupção simulada somente no banco descartável, fora das validações da API.
        jdbc.update("UPDATE pagamento SET valor=600 WHERE id=?", p2);
        long auditoria = jdbc.queryForObject("SELECT COUNT(*) FROM auditoria_evento", Long.class);
        var saldo = consultarSaldo(projeto);
        verificarOcorrencia(saldo, "PAGAMENTOS_EXCEDEM_COMPROMISSO", "pagamento", p1);
        verificarOcorrencia(saldo, "PAGAMENTOS_EXCEDEM_COMPROMISSO", "pagamento", p2);
        assertThat(saldo.get("inconsistencias").get(0).get("compromissoId").asLong()).isEqualTo(co);
        assertThat(new BigDecimal(saldo.get("saldoComprometido").asText()))
                .isEqualByComparingTo("-100");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT valor FROM pagamento WHERE id=?", BigDecimal.class, p2))
                .isEqualByComparingTo("600");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria_evento", Long.class))
                .isEqualTo(auditoria);
    }

    @Test
    void saldoGlobalNaoOcultaDeficitDeProjetoEFiltraOcorrencias() throws Exception {
        aporte("1000");
        aprovar(contrato());
        jdbc.update("UPDATE aporte SET valor=100 WHERE projeto_id=?", projeto);
        long outro =
                db.inserir(
                        "INSERT INTO projeto(nome,origem_recursos,ativo) VALUES ('Outro','Fundo',TRUE)");
        chamar(
                post("/api/v1/projetos/" + outro + "/aportes"),
                gestor,
                Map.of(
                        "tipo",
                        "APORTE",
                        "valor",
                        "5000",
                        "dataAporte",
                        LocalDate.now().toString(),
                        "descricao",
                        "Outro projeto"),
                201);
        var global = consultarSaldo(null);
        assertThat(new BigDecimal(global.get("saldoDisponivel").asText())).isPositive();
        verificarOcorrencia(global, "RECURSOS_INSUFICIENTES", "projeto", projeto);
        verificarOcorrencia(consultarSaldo(projeto), "RECURSOS_INSUFICIENTES", "projeto", projeto);
        assertThat(consultarSaldo(outro).get("consolidacaoCompleta").asBoolean()).isTrue();
    }

    @Test
    void saldoGlobalIdentificaPagamentoOrfaoMesmoQuandoAgregadoNaoConsegueIncluiLo()
            throws Exception {
        aporte("1000");
        long co = aprovar(contrato());
        long p =
                chamar(
                                post("/api/v1/pagamentos"),
                                gestor,
                                pagamento(co, "100", "conferencia-orfao-01"),
                                201)
                        .get("id")
                        .asLong();
        // Simula importação defeituosa. Desliga FK somente nesta conexão do banco descartável.
        jdbc.execute(
                (ConnectionCallback<Void>)
                        connection -> {
                            try (var statement = connection.createStatement()) {
                                statement.execute("SET FOREIGN_KEY_CHECKS=0");
                                try (var update =
                                        connection.prepareStatement(
                                                "UPDATE pagamento SET compromisso_id=9223372036854775807 WHERE id=?")) {
                                    update.setLong(1, p);
                                    update.executeUpdate();
                                } finally {
                                    statement.execute("SET FOREIGN_KEY_CHECKS=1");
                                }
                            }
                            return null;
                        });
        var saldo = consultarSaldo(null);
        verificarOcorrencia(saldo, "PAGAMENTO_INVALIDO", "pagamento", p);
        var ocorrencia = saldo.get("inconsistencias").get(0);
        assertThat(ocorrencia.get("projetoId").isNull()).isTrue();
        assertThat(ocorrencia.get("contratoId").isNull()).isTrue();
        assertThat(ocorrencia.get("compromissoId").asLong()).isEqualTo(Long.MAX_VALUE);
        assertThat(new BigDecimal(saldo.get("totalPago").asText())).isZero();
        assertThat(consultarSaldo(projeto).get("inconsistencias").size()).isZero();
    }

    @Test
    void saldoIdentificaReservaAusenteValorExercicioEContratoNaoAprovado() throws Exception {
        aporte("1000");
        long c = contrato(), co = aprovar(c);
        jdbc.update("UPDATE compromisso_anual SET valor=700,exercicio=exercicio+1 WHERE id=?", co);
        var saldo = consultarSaldo(projeto);
        verificarOcorrencia(saldo, "COMPROMISSO_INVALIDO", "compromisso_anual", co);
        verificarOcorrencia(saldo, "CONTRATO_SEM_COMPROMISSO", "contrato", c);
        jdbc.update("UPDATE compromisso_anual SET valor=800,exercicio=exercicio-1 WHERE id=?", co);
        long p =
                chamar(
                                post("/api/v1/pagamentos"),
                                gestor,
                                pagamento(co, "100", "conferencia-000003"),
                                201)
                        .get("id")
                        .asLong();
        jdbc.update("UPDATE contrato SET status='RASCUNHO' WHERE id=?", c);
        saldo = consultarSaldo(projeto);
        verificarOcorrencia(saldo, "COMPROMISSO_INVALIDO", "compromisso_anual", co);
        verificarOcorrencia(saldo, "PAGAMENTO_INVALIDO", "pagamento", p);
    }

    @Test
    void saldoIdentificaOrigemAusenteSaldoInicialDuplicadoEVinculoDivergente() throws Exception {
        aporte("1000");
        long c = contrato();
        aprovar(c);
        long primeiro =
                jdbc.queryForObject(
                        "SELECT id FROM aporte WHERE projeto_id=?", Long.class, projeto);
        long segundo =
                db.inserir(
                        "INSERT INTO aporte(projeto_id,tipo,data_aporte,valor,descricao,criado_em,criado_por) SELECT projeto_id,tipo,data_aporte,valor,'Duplicado',criado_em,criado_por FROM aporte WHERE id=?",
                        primeiro);
        jdbc.update("UPDATE projeto SET origem_recursos='' WHERE id=?", projeto);
        long outro =
                chamar(
                                post("/api/v1/produtores"),
                                tecnico,
                                Map.of("nome", "Outro produtor", "telefone", "51"),
                                201)
                        .get("id")
                        .asLong();
        jdbc.update("UPDATE contrato SET produtor_id=? WHERE id=?", outro, c);
        var saldo = consultarSaldo(projeto);
        verificarOcorrencia(saldo, "SALDO_INICIAL_DUPLICADO", "aporte", primeiro);
        verificarOcorrencia(saldo, "SALDO_INICIAL_DUPLICADO", "aporte", segundo);
        verificarOcorrencia(saldo, "APORTE_INVALIDO", "aporte", primeiro);
        verificarOcorrencia(saldo, "CONTRATO_VINCULO_INVALIDO", "contrato", c);
    }

    @Test
    void fluxoCompletoReservaPagamentoParcialRetryEExtrato() throws Exception {
        aporte("1000.00");
        long c = contrato(), co = aprovar(c);
        var saldo =
                chamar(
                        get("/api/v1/financeiro/saldo").param("projetoId", Long.toString(projeto)),
                        gestor,
                        null,
                        200);
        assertThat(new BigDecimal(saldo.get("saldoDisponivel").asText()))
                .isEqualByComparingTo("200");
        var entrada = pagamento(co, "300.00", "operacao-teste-0001");
        long pagamento =
                chamar(post("/api/v1/pagamentos"), gestor, entrada, 201).get("id").asLong();
        assertThat(chamar(post("/api/v1/pagamentos"), gestor, entrada, 200).get("id").asLong())
                .isEqualTo(pagamento);
        chamar(
                post("/api/v1/pagamentos"),
                gestor,
                pagamento(co, "301.00", "operacao-teste-0001"),
                409);
        chamar(
                post("/api/v1/pagamentos"),
                gestor,
                pagamento(co, "500.00", "operacao-teste-0002"),
                201);
        chamar(post("/api/v1/pagamentos"), gestor, entrada, 200);
        chamar(
                post("/api/v1/pagamentos"),
                gestor,
                pagamento(co, "0.01", "operacao-teste-0003"),
                409);
        saldo = chamar(get("/api/v1/financeiro/saldo"), gestor, null, 200);
        assertThat(new BigDecimal(saldo.get("saldoCaixa").asText())).isEqualByComparingTo("200");
        assertThat(new BigDecimal(saldo.get("saldoComprometido").asText())).isZero();
        assertThat(saldo.get("consolidacaoCompleta").asBoolean()).isTrue();
        assertThat(saldo.get("inconsistencias").size()).isZero();
        chamar(get("/api/v1/pagamentos/" + pagamento), produtor, null, 200);
        assertThat(
                        chamar(get("/api/v1/meu-historico"), produtor, null, 200)
                                .get("totalItems")
                                .asLong())
                .isEqualTo(1);
        var relatorio =
                chamar(
                        get("/api/v1/relatorios/aplicacao-recursos").param("size", "1"),
                        gestor,
                        null,
                        200);
        assertThat(relatorio.get("items").size()).isEqualTo(1);
        assertThat(new BigDecimal(relatorio.get("totalPago").asText())).isEqualByComparingTo("800");
        byte[] pdf =
                mvc.perform(
                                get("/api/v1/contratos/" + c + "/documento")
                                        .with(authentication(produtor)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();
        try (var document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isPositive();
            assertThat(new PDFTextStripper().getText(document)).contains("800.00");
        }
    }

    @Test
    void negaAcessoPorPerfilEPorPropriedade() throws Exception {
        chamar(get("/api/v1/produtores"), admin, null, 403);
        chamar(post("/api/v1/produtores"), gestor, Map.of("nome", "Outro", "telefone", "51"), 403);
        long outro =
                chamar(
                                post("/api/v1/produtores"),
                                tecnico,
                                Map.of("nome", "Outro", "telefone", "51"),
                                201)
                        .get("id")
                        .asLong();
        chamar(get("/api/v1/produtores/" + outro), produtor, null, 404);
        chamar(get("/api/v1/financeiro/saldo"), produtor, null, 403);
        var usuario = chamar(get("/api/v1/usuarios"), admin, null, 200);
        assertThat(usuario.toString()).doesNotContain("senha", "hash");
        chamar(get("/api/v1/produtores").param("size", "101"), gestor, null, 400);
        chamar(get("/api/v1/propriedades/" + propriedade), produtor, null, 200);
    }

    @Test
    void exigeDiagnosticoCompletoEPreservaCalculoAnterior() throws Exception {
        long d = diagnostico();
        chamar(post("/api/v1/diagnosticos/" + d + "/concluir"), tecnico, null, 422);
        assertThat(
                        chamar(get("/api/v1/diagnosticos/" + d), tecnico, null, 200)
                                .get("status")
                                .asText())
                .isEqualTo("RASCUNHO");
        long calc = calculo();
        var anterior = chamar(get("/api/v1/calculos/" + calc), gestor, null, 200);
        configurar("30000.00");
        assertThat(chamar(get("/api/v1/calculos/" + calc), gestor, null, 200)).isEqualTo(anterior);
        long diagnostico = anterior.get("diagnosticoId").asLong();
        chamar(
                post("/api/v1/diagnosticos/" + diagnostico + "/abrir-correcao"),
                tecnico,
                Map.of("motivo", "Correção de teste"),
                200);
        chamar(
                post("/api/v1/calculos"),
                gestor,
                Map.of("diagnosticoId", diagnostico, "areaContratada", "1.0"),
                409);
        assertThat(chamar(get("/api/v1/calculos/" + calc), gestor, null, 200)).isEqualTo(anterior);
    }

    @Test
    void impedeAprovacaoSemSaldoESnapshotDesatualizado() throws Exception {
        long c = contrato();
        chamar(
                post("/api/v1/contratos/" + c + "/aprovar"),
                gestor,
                Map.of("exercicio", LocalDate.now().getYear()),
                409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM compromisso_anual", Long.class))
                .isZero();
        aporte("1000");
        chamar(
                put("/api/v1/produtores/" + produtorId),
                tecnico,
                Map.of(
                        "nome",
                        "Nome corrigido",
                        "documento",
                        "12345678901",
                        "endereco",
                        "Outro endereço",
                        "telefone",
                        "51"),
                200);
        chamar(
                post("/api/v1/contratos/" + c + "/aprovar"),
                gestor,
                Map.of("exercicio", LocalDate.now().getYear()),
                409);
        chamar(post("/api/v1/contratos/" + c + "/gerar-documento"), gestor, null, 200);
        aprovar(c);
        chamar(
                post("/api/v1/contratos/" + c + "/aprovar"),
                gestor,
                Map.of("exercicio", LocalDate.now().getYear()),
                409);
        chamar(post("/api/v1/contratos/" + c + "/gerar-documento"), gestor, null, 409);
    }

    @Test
    void rejeitaArquivoFalsoESituacaoHidricaIncoerente() throws Exception {
        chamar(
                multipart("/api/v1/propriedades/" + propriedade + "/croquis")
                        .file(
                                new MockMultipartFile(
                                        "arquivo",
                                        "fake.pdf",
                                        "application/pdf",
                                        "nao-pdf".getBytes()))
                        .param("areaPsa", "1"),
                tecnico,
                null,
                415);
        chamar(
                put("/api/v1/propriedades/" + propriedade + "/levantamento-hidrico"),
                tecnico,
                Map.of("situacao", "AUSENTES"),
                200);
        chamar(
                post("/api/v1/propriedades/" + propriedade + "/fontes-hidricas"),
                tecnico,
                Map.of("tipo", "NASCENTE", "protecao", "PROTEGIDA", "ativo", true),
                201);
        chamar(
                put("/api/v1/propriedades/" + propriedade + "/levantamento-hidrico"),
                tecnico,
                Map.of("situacao", "AUSENTES"),
                409);
    }

    @Test
    void aprovacoesConcorrentesNaoReservamMaisQueOSaldo() throws Exception {
        aporte("1000.00");
        long a = contrato(), b = contrato();
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var fa = executor.submit(() -> aprovarConcorrente(a, inicio));
            var fb = executor.submit(() -> aprovarConcorrente(b, inicio));
            inicio.countDown();
            assertThat(List.of(fa.get(20, TimeUnit.SECONDS), fb.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT SUM(valor) FROM compromisso_anual", BigDecimal.class))
                .isEqualByComparingTo("800");
    }

    boolean aprovarConcorrente(long id, CountDownLatch inicio) throws Exception {
        inicio.await();
        SecurityContextHolder.getContext().setAuthentication(gestor);
        try {
            financeiro.aprovar(id, new Aprovacao(LocalDate.now().getYear()));
            return true;
        } catch (FalhaApi e) {
            assertThat(e.status()).isEqualTo(409);
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void pagamentosConcorrentesComMesmaChaveGeramUmLancamento() throws Exception {
        aporte("1000");
        long co = aprovar(contrato());
        var p =
                new Pagamento(
                        co,
                        projeto,
                        new BigDecimal("800.00"),
                        LocalDate.now(),
                        "Teste",
                        "concorrente-0000001",
                        null);
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<ResultadoPagamento> tarefa =
                    () -> {
                        inicio.await();
                        SecurityContextHolder.getContext().setAuthentication(gestor);
                        try {
                            return financeiro.pagar(p);
                        } finally {
                            SecurityContextHolder.clearContext();
                        }
                    };
            var a = executor.submit(tarefa);
            var b = executor.submit(tarefa);
            inicio.countDown();
            var ra = a.get(20, TimeUnit.SECONDS);
            var rb = b.get(20, TimeUnit.SECONDS);
            assertThat(ra.pagamento().get("id")).isEqualTo(rb.pagamento().get("id"));
            assertThat(ra.repetido()).isNotEqualTo(rb.repetido());
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pagamento", Long.class)).isEqualTo(1);
    }
}
