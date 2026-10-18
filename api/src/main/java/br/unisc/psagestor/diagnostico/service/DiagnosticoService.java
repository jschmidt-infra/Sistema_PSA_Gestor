package br.unisc.psagestor.diagnostico.service;

import br.unisc.psagestor.acesso.service.AuditoriaService;
import br.unisc.psagestor.cadastros.repository.PropriedadeRepository;
import br.unisc.psagestor.calculo.repository.CalculoPsaRepository;
import br.unisc.psagestor.calculo.service.CalculadoraPsa;
import br.unisc.psagestor.comum.api.Dados;
import br.unisc.psagestor.comum.api.FalhaApi;
import br.unisc.psagestor.comum.api.RegraNegocioException;
import br.unisc.psagestor.comum.api.SessaoAtual;
import br.unisc.psagestor.configuracao.repository.ParametroProjetoRepository;
import br.unisc.psagestor.configuracao.repository.ProjetoRepository;
import br.unisc.psagestor.diagnostico.repository.DiagnosticoConsultaRepository;
import br.unisc.psagestor.diagnostico.repository.DiagnosticoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Coordena versões, respostas e cálculos sem reinterpretar avaliações históricas. */
@Service
public class DiagnosticoService {
    private static final String METODOLOGIA_PENDENTE = "PENDENTE";
    private static final String METODOLOGIA_DEMONSTRACAO = "DEMONSTRACAO_SOMA_PONDERADA";
    private static final String GRUPO_LEGADO = "LEGADO_SEM_GRUPO";
    private static final int MAXIMO_PERGUNTAS = 100;
    private static final int ESCALA_PONTUACAO = 6;
    private final PropriedadeRepository propriedadeRepo;
    private final ProjetoRepository projetoRepo;
    private final ParametroProjetoRepository parametroProjetoRepo;
    private final DiagnosticoRepository diagnosticoRepo;
    private final CalculoPsaRepository calculoPsaRepo;
    private final AuditoriaService auditoria;
    private final DiagnosticoConsultaRepository consultas;
    private final JsonMapper json;
    private final CalculadoraPsa calculadora;
    private final Environment env;

    public DiagnosticoService(
            JsonMapper json,
            CalculadoraPsa calculadora,
            Environment env,
            DiagnosticoConsultaRepository consultas,
            PropriedadeRepository propriedadeRepo,
            ProjetoRepository projetoRepo,
            ParametroProjetoRepository parametroProjetoRepo,
            DiagnosticoRepository diagnosticoRepo,
            CalculoPsaRepository calculoPsaRepo,
            AuditoriaService auditoria) {
        this.propriedadeRepo = propriedadeRepo;
        this.projetoRepo = projetoRepo;
        this.parametroProjetoRepo = parametroProjetoRepo;
        this.diagnosticoRepo = diagnosticoRepo;
        this.calculoPsaRepo = calculoPsaRepo;
        this.auditoria = auditoria;
        this.consultas = consultas;
        this.json = json;
        this.calculadora = calculadora;
        this.env = env;
    }

    public record Alternativa(
            @NotBlank @Size(max = 60) String id,
            @NotBlank @Size(max = 300) String texto,
            @NotNull @DecimalMin("0") @DecimalMax("1") @Digits(integer = 1, fraction = 6)
                    BigDecimal valor) {}

    public record Pergunta(
            @NotBlank @Size(max = 60) String id,
            @NotBlank @Size(max = 500) String texto,
            boolean obrigatoria,
            @NotNull @DecimalMin("0.000001") @DecimalMax("1") @Digits(integer = 1, fraction = 6)
                    BigDecimal peso,
            @NotEmpty @Size(max = 20) List<@NotNull @Valid Alternativa> alternativas) {}

    public record Grupo(
            @NotBlank @Size(max = 60) String id,
            @NotBlank @Size(max = 200) String nome,
            @NotEmpty @Size(max = 100) List<@NotNull @Valid Pergunta> perguntas) {}

    public record Parametros(
            @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2)
                    BigDecimal valorHectare,
            @NotNull @DecimalMin("0") @DecimalMax("1") @Digits(integer = 1, fraction = 6)
                    BigDecimal percentual,
            @NotBlank String metodologia,
            @NotNull @Size(max = 100) List<@NotNull @Valid Grupo> grupos) {
        public List<Pergunta> perguntas() {
            return grupos.stream().flatMap(g -> g.perguntas().stream()).toList();
        }
    }

    public record Criar(
            @NotNull @Positive Long projetoId, @NotNull @PastOrPresent LocalDate dataVisita) {}

    public record Resposta(@NotBlank String perguntaId, @NotBlank String alternativaId) {}

    public record Respostas(@NotNull @Size(max = 100) List<@NotNull @Valid Resposta> respostas) {}

    public record Correcao(@NotBlank @Size(max = 2000) String motivo) {}

    public record Calculo(
            @NotNull @Positive Long diagnosticoId,
            @NotNull @DecimalMin("0.0001") @Digits(integer = 8, fraction = 4)
                    BigDecimal areaContratada) {}

    public Map<String, Object> parametros(long projeto) {
        projetoRepo.obterMapa(projeto);
        return parametroProjetoRepo
                .findFirstByProjetoIdOrderByVersaoDesc(projeto)
                .orElseThrow(FalhaApi::inexistente)
                .dados();
    }

    @Transactional
    public Object configurar(long projeto, Parametros p) {
        projetoRepo.bloquearMapa(projeto);
        validarConfiguracao(p);
        int versao = parametroProjetoRepo.proximaVersao(projeto);
        long id =
                parametroProjetoRepo.criar(
                        projeto,
                        versao,
                        p.valorHectare(),
                        p.percentual(),
                        json.writeValueAsString(p),
                        SessaoAtual.id());
        auditoria.registrar("VERSIONAR", "parametro_projeto", id);
        return Dados.saida(parametroProjetoRepo.obterMapa(id));
    }

    private boolean demonstracaoPermitida(String metodologia) {
        return METODOLOGIA_DEMONSTRACAO.equals(metodologia)
                && env.matchesProfiles("test", "demonstracao")
                && !env.matchesProfiles("prod");
    }

    private void validarConfiguracao(Parametros parametros) {
        boolean pendente = METODOLOGIA_PENDENTE.equals(parametros.metodologia());
        if (!pendente && !demonstracaoPermitida(parametros.metodologia())) {
            throw new RegraNegocioException(
                    "METODOLOGIA_PENDENTE",
                    "A metodologia oficial ainda não foi definida. A demonstração só pode ser usada no ambiente de testes.");
        }
        validarGrupos(parametros.grupos());
        var perguntas = parametros.perguntas();
        if (perguntas.size() > MAXIMO_PERGUNTAS) {
            throw new RegraNegocioException(
                    "LIMITE_PERGUNTAS",
                    "O questionário deve ter no máximo 100 perguntas no total.");
        }
        BigDecimal somaPesos = validarPerguntas(perguntas);
        if (!pendente && somaPesos.compareTo(BigDecimal.ONE) != 0) {
            throw new RegraNegocioException(
                    "PESOS_INVALIDOS", "Na demonstração, a soma dos pesos deve ser 1.");
        }
        if (pendente && !parametros.grupos().isEmpty()) {
            throw new RegraNegocioException(
                    "METODOLOGIA_PENDENTE",
                    "Use uma lista de grupos vazia enquanto o questionário não estiver definido.");
        }
    }

    private void validarGrupos(List<Grupo> grupos) {
        Set<String> identificadores = new HashSet<>();
        for (var grupo : grupos) {
            if (GRUPO_LEGADO.equals(grupo.id())) {
                throw new RegraNegocioException(
                        "GRUPO_RESERVADO",
                        "Classifique as perguntas históricas em grupos de indicadores antes de criar uma nova versão.");
            }
            if (!identificadores.add(grupo.id())) {
                throw new RegraNegocioException(
                        "GRUPO_DUPLICADO", "Identificador de grupo repetido.");
            }
        }
    }

    private BigDecimal validarPerguntas(List<Pergunta> perguntas) {
        Set<String> identificadores = new HashSet<>();
        BigDecimal somaPesos = BigDecimal.ZERO;
        for (var pergunta : perguntas) {
            if (!identificadores.add(pergunta.id())) {
                throw new RegraNegocioException(
                        "PERGUNTA_DUPLICADA", "Identificador de pergunta repetido.");
            }
            Set<String> alternativas = new HashSet<>();
            for (var alternativa : pergunta.alternativas()) {
                if (!alternativas.add(alternativa.id())) {
                    throw new RegraNegocioException(
                            "ALTERNATIVA_DUPLICADA", "Identificador de alternativa repetido.");
                }
            }
            somaPesos = somaPesos.add(pergunta.peso());
        }
        return somaPesos;
    }

    @Transactional
    public long criar(long propriedade, Criar c) {
        propriedadeRepo.obterMapa(propriedade);
        // A versão fica fixada na criação; mudanças futuras só valem para novos diagnósticos.
        var p = parametros(c.projetoId());
        long id =
                diagnosticoRepo.criar(
                        propriedade,
                        c.projetoId(),
                        ((Number) p.get("id")).longValue(),
                        SessaoAtual.id(),
                        c.dataVisita(),
                        SessaoAtual.id());
        auditoria.registrar("CRIAR", "diagnostico", id);
        return id;
    }

    // Adaptação somente de leitura: versões históricas não são reescritas nem reclassificadas.
    private Parametros lerConfiguracao(String conteudo) {
        var raiz = json.readTree(conteudo);
        if (raiz.has("grupos")) return json.treeToValue(raiz, Parametros.class);
        var perguntas = Arrays.asList(json.treeToValue(raiz.get("perguntas"), Pergunta[].class));
        return new Parametros(
                new BigDecimal(raiz.get("valorHectare").asText()),
                new BigDecimal(raiz.get("percentual").asText()),
                raiz.get("metodologia").asText(),
                perguntas.isEmpty()
                        ? List.of()
                        : List.of(
                                new Grupo(
                                        GRUPO_LEGADO,
                                        "Perguntas históricas sem classificação",
                                        perguntas)));
    }

    private Parametros configuracao(Map<String, Object> d) {
        return lerConfiguracao(
                (String)
                        parametroProjetoRepo
                                .obterMapa(Dados.numero(d, "parametro_id"))
                                .get("configuracao_json"));
    }

    public Object questionario(long id) {
        var d = diagnosticoRepo.obterMapa(id);
        return Map.of("parametroId", d.get("parametro_id"), "configuracao", configuracao(d));
    }

    private Map<String, Resposta> validarRespostas(
            Parametros p, List<Resposta> respostas, boolean concluir) {
        Map<String, Pergunta> perguntas = new HashMap<>();
        p.perguntas().forEach(q -> perguntas.put(q.id(), q));
        Map<String, Resposta> mapa = new HashMap<>();
        for (var r : respostas) {
            var q = perguntas.get(r.perguntaId());
            if (q == null
                    || q.alternativas().stream().noneMatch(a -> a.id().equals(r.alternativaId()))
                    || mapa.put(r.perguntaId(), r) != null)
                throw new RegraNegocioException(
                        "RESPOSTA_INVALIDA", "Pergunta ou alternativa inválida ou repetida.");
        }
        if (concluir
                && p.perguntas().stream()
                        .anyMatch(q -> q.obrigatoria() && !mapa.containsKey(q.id())))
            throw new RegraNegocioException(
                    "DIAGNOSTICO_INCOMPLETO", "Responda às perguntas obrigatórias.");
        return mapa;
    }

    @Transactional
    public void responder(long id, Respostas entrada) {
        var d = diagnosticoRepo.bloquearMapa(id);
        if ("CONCLUIDO".equals(d.get("status")))
            throw FalhaApi.conflito("Abra uma correção antes de alterar as respostas.");
        validarRespostas(configuracao(d), entrada.respostas(), false);
        diagnosticoRepo.registrarRespostas(json.writeValueAsString(entrada.respostas()), id);
        auditoria.registrar("RESPONDER", "diagnostico", id);
    }

    @Transactional
    public void concluir(long id, boolean reapurar) {
        var d = diagnosticoRepo.bloquearMapa(id);
        if (reapurar != "CONCLUIDO".equals(d.get("status")))
            throw FalhaApi.conflito(
                    reapurar
                            ? "Conclua o diagnóstico antes de reapurar."
                            : "Diagnóstico já concluído.");
        var p = configuracao(d);
        var respostas =
                Arrays.asList(json.readValue((String) d.get("respostas_json"), Resposta[].class));
        var mapa = validarRespostas(p, respostas, true);
        if (!demonstracaoPermitida(p.metodologia())) {
            throw new RegraNegocioException(
                    "METODOLOGIA_PENDENTE",
                    "A apuração oficial ainda depende da definição do questionário.");
        }
        BigDecimal pontuacao = apurarPontuacao(p.perguntas(), mapa);
        String memoria =
                json.writeValueAsString(
                        Map.of(
                                "metodologia",
                                p.metodologia(),
                                "parametroId",
                                d.get("parametro_id"),
                                "revisao",
                                d.get("revisao"),
                                "respostas",
                                respostas,
                                "grupos",
                                p.grupos(),
                                "pontuacao",
                                pontuacao.toPlainString(),
                                "demonstracao",
                                true));
        diagnosticoRepo.concluir(pontuacao, memoria, id);
        auditoria.registrar(reapurar ? "REAPURAR" : "CONCLUIR", "diagnostico", id);
    }

    private BigDecimal apurarPontuacao(List<Pergunta> perguntas, Map<String, Resposta> respostas) {
        BigDecimal pontuacao = BigDecimal.ZERO;
        for (var pergunta : perguntas) {
            var resposta = respostas.get(pergunta.id());
            if (resposta == null) {
                // Obrigatórias já foram validadas; opcionais sem resposta contribuem zero.
                continue;
            }
            var alternativa =
                    pergunta.alternativas().stream()
                            .filter(item -> item.id().equals(resposta.alternativaId()))
                            .findFirst()
                            .orElseThrow();
            pontuacao = pontuacao.add(pergunta.peso().multiply(alternativa.valor()));
        }
        return pontuacao.setScale(ESCALA_PONTUACAO, RoundingMode.HALF_UP);
    }

    @Transactional
    public void corrigir(long id, Correcao c) {
        var d = diagnosticoRepo.bloquearMapa(id);
        if (!"CONCLUIDO".equals(d.get("status")))
            throw FalhaApi.conflito("Apenas diagnósticos concluídos podem ser reabertos.");
        diagnosticoRepo.reabrir(c.motivo(), id);
        auditoria.registrar("ABRIR_CORRECAO", "diagnostico", id);
    }

    @Transactional
    public long calcular(Calculo c) {
        var d = diagnosticoRepo.bloquearMapa(c.diagnosticoId());
        if (!"CONCLUIDO".equals(d.get("status")) || d.get("pontuacao") == null)
            throw FalhaApi.conflito("Conclua e apure o diagnóstico antes de calcular.");
        var propriedade = propriedadeRepo.bloquearMapa(Dados.numero(d, "propriedade_id"));
        var p = parametros(Dados.numero(d, "projeto_id"));
        var valor =
                calculadora.calcular(
                        Dados.decimal(p, "valor_hectare"),
                        Dados.decimal(p, "percentual"),
                        Dados.decimal(d, "pontuacao"),
                        c.areaContratada(),
                        Dados.decimal(propriedade, "area_total"));
        long id =
                calculoPsaRepo.criar(
                        c.diagnosticoId(),
                        ((Number) p.get("id")).longValue(),
                        ((Number) d.get("revisao")).intValue(),
                        (String) d.get("memoria_json"),
                        c.areaContratada(),
                        (BigDecimal) p.get("valor_hectare"),
                        (BigDecimal) p.get("percentual"),
                        (BigDecimal) d.get("pontuacao"),
                        valor,
                        SessaoAtual.id());
        auditoria.registrar("CALCULAR", "calculo_psa", id);
        return id;
    }

    public Object consultarParametros(long projeto) {
        return Dados.saida(parametros(projeto));
    }

    public Object listar(long propriedade, int page, int size) {
        verificarTitular(propriedade);
        return consultas.listar(propriedade, page, size);
    }

    public Object consultar(long id) {
        verificarTitular(Dados.numero(diagnosticoRepo.obterMapa(id), "propriedade_id"));
        return consultas.buscar(id);
    }

    public Object consultarCalculo(long id) {
        var calculo = calculoPsaRepo.obterMapa(id);
        verificarTitular(
                Dados.numero(
                        diagnosticoRepo.obterMapa(Dados.numero(calculo, "diagnostico_id")),
                        "propriedade_id"));
        return consultas.calculo(id);
    }

    private void verificarTitular(long propriedade) {
        SessaoAtual.verificarProdutor(
                Dados.numero(propriedadeRepo.obterMapa(propriedade), "produtor_id"));
    }
}
