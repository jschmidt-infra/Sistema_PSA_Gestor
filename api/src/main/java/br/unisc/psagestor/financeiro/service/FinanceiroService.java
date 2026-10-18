package br.unisc.psagestor.financeiro.service;

import br.unisc.psagestor.acesso.service.AuditoriaService;
import br.unisc.psagestor.arquivos.repository.CroquiRepository;
import br.unisc.psagestor.cadastros.repository.ProdutorRepository;
import br.unisc.psagestor.cadastros.repository.PropriedadeRepository;
import br.unisc.psagestor.calculo.repository.CalculoPsaRepository;
import br.unisc.psagestor.comum.api.Dados;
import br.unisc.psagestor.comum.api.FalhaApi;
import br.unisc.psagestor.comum.api.RegraNegocioException;
import br.unisc.psagestor.comum.api.SessaoAtual;
import br.unisc.psagestor.configuracao.repository.ProjetoRepository;
import br.unisc.psagestor.contratos.repository.ContratoRepository;
import br.unisc.psagestor.contratos.service.ContratoService;
import br.unisc.psagestor.diagnostico.repository.DiagnosticoRepository;
import br.unisc.psagestor.financeiro.repository.AporteRepository;
import br.unisc.psagestor.financeiro.repository.CompromissoAnualRepository;
import br.unisc.psagestor.financeiro.repository.FinanceiroConsultaRepository;
import br.unisc.psagestor.financeiro.repository.PagamentoRepository;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceiroService {
    private final PropriedadeRepository propriedadeRepo;
    private final ProjetoRepository projetoRepo;
    private final ProdutorRepository produtorRepo;
    private final PagamentoRepository pagamentoRepo;
    private final DiagnosticoRepository diagnosticoRepo;
    private final CroquiRepository croquiRepo;
    private final ContratoRepository contratoRepo;
    private final CompromissoAnualRepository compromissoAnualRepo;
    private final CalculoPsaRepository calculoPsaRepo;
    private final AporteRepository aporteRepo;
    private final AuditoriaService auditoria;
    private final FinanceiroConsultaRepository consultas;
    private final ContratoService contratos;
    private final ConferenciaFinanceira conferencia;

    public FinanceiroService(
            ContratoService contratos,
            ConferenciaFinanceira conferencia,
            FinanceiroConsultaRepository consultas,
            PropriedadeRepository propriedadeRepo,
            ProjetoRepository projetoRepo,
            ProdutorRepository produtorRepo,
            PagamentoRepository pagamentoRepo,
            DiagnosticoRepository diagnosticoRepo,
            CroquiRepository croquiRepo,
            ContratoRepository contratoRepo,
            CompromissoAnualRepository compromissoAnualRepo,
            CalculoPsaRepository calculoPsaRepo,
            AporteRepository aporteRepo,
            AuditoriaService auditoria) {
        this.propriedadeRepo = propriedadeRepo;
        this.projetoRepo = projetoRepo;
        this.produtorRepo = produtorRepo;
        this.pagamentoRepo = pagamentoRepo;
        this.diagnosticoRepo = diagnosticoRepo;
        this.croquiRepo = croquiRepo;
        this.contratoRepo = contratoRepo;
        this.compromissoAnualRepo = compromissoAnualRepo;
        this.calculoPsaRepo = calculoPsaRepo;
        this.aporteRepo = aporteRepo;
        this.auditoria = auditoria;
        this.consultas = consultas;
        this.contratos = contratos;
        this.conferencia = conferencia;
    }

    public enum TipoAporte {
        SALDO_INICIAL,
        APORTE
    }

    public record Aporte(
            @NotNull TipoAporte tipo,
            @NotNull @PastOrPresent LocalDate dataAporte,
            @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal valor,
            @NotBlank @Size(max = 255) String descricao) {}

    public record Aprovacao(@Min(2000) @Max(9999) int exercicio) {}

    public record Pagamento(
            @NotNull @Positive Long compromissoId,
            @NotNull @Positive Long projetoId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal valor,
            @NotNull @PastOrPresent LocalDate dataPagamento,
            @NotBlank @Size(max = 100) String referencia,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{16,64}") String chaveOperacao,
            @Size(max = 2000) String observacao) {}

    public record ResultadoPagamento(boolean repetido, Map<String, Object> pagamento) {}

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long aporte(long projeto, Aporte a) {
        projetoRepo.bloquearMapa(projeto);
        if (a.tipo() == TipoAporte.SALDO_INICIAL
                && aporteRepo.quantidadeSaldosIniciais(projeto) > 0)
            throw FalhaApi.conflito("Saldo inicial já registrado para o projeto.");
        long id =
                aporteRepo.criar(
                        projeto,
                        a.tipo().name(),
                        a.dataAporte(),
                        a.valor(),
                        a.descricao(),
                        SessaoAtual.id());
        auditoria.registrar("REGISTRAR", "aporte", id);
        return id;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Map<String, Object> saldo(Long projeto) {
        if (projeto != null) projetoRepo.obterMapa(projeto);
        var valores = totais(projeto);
        var inconsistencias = conferencia.verificar(projeto);
        var resultado = new LinkedHashMap<String, Object>(valores);
        resultado.put("consolidacaoCompleta", inconsistencias.isEmpty());
        resultado.put("statusConsolidacao", inconsistencias.isEmpty() ? "COMPLETA" : "INCOMPLETA");
        resultado.put(
                "mensagem",
                inconsistencias.isEmpty()
                        ? "Consolidação financeira concluída."
                        : "Consolidação financeira incompleta. Os totais são indicativos; revise os registros identificados antes de utilizá-los.");
        resultado.put("quantidadeInconsistencias", inconsistencias.size());
        resultado.put("inconsistencias", inconsistencias);
        return resultado;
    }

    private Map<String, String> totais(Long projeto) {
        var s = consultas.totais(projeto);
        var recursos = Dados.decimal(s, "recursos");
        var reservas = Dados.decimal(s, "reservas");
        var pagos = Dados.decimal(s, "pagos");
        return Map.of(
                "recursos",
                recursos.toPlainString(),
                "totalPago",
                pagos.toPlainString(),
                "saldoCaixa",
                recursos.subtract(pagos).toPlainString(),
                "saldoComprometido",
                reservas.subtract(pagos).toPlainString(),
                "saldoDisponivel",
                recursos.subtract(reservas).toPlainString());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void aprovar(long id, Aprovacao a) {
        var consulta = contratoRepo.obterMapa(id);
        long projeto = Dados.numero(consulta, "projeto_id");
        projetoRepo.bloquearMapa(projeto);
        var c = contratoRepo.bloquearMapa(id);
        contratos.rascunho(c);
        var calculoBloqueio = calculoPsaRepo.obterMapa(Dados.numero(c, "calculo_id"));
        diagnosticoRepo.bloquearMapa(Dados.numero(calculoBloqueio, "diagnostico_id"));
        propriedadeRepo.bloquearMapa(Dados.numero(c, "propriedade_id"));
        produtorRepo.bloquearMapa(Dados.numero(c, "produtor_id"));
        if (c.get("croqui_id") != null) croquiRepo.bloquearMapa(Dados.numero(c, "croqui_id"));
        if (c.get("documento_chave") == null
                || !contratos.snapshot(c).equals(c.get("documento_snapshot")))
            throw FalhaApi.conflito(
                    "Gere novamente o documento; ele está ausente ou os dados mudaram.");
        LocalDate inicio = LocalDate.parse(c.get("inicio").toString()),
                fim = LocalDate.parse(c.get("fim").toString());
        if (a.exercicio() != inicio.getYear() || a.exercicio() > fim.getYear())
            throw new RegraNegocioException(
                    "EXERCICIO_INVALIDO",
                    "O compromisso inicial deve corresponder ao ano de início da vigência.");
        var calc = calculoPsaRepo.obterMapa(Dados.numero(c, "calculo_id"));
        var valor = Dados.decimal(calc, "valor_anual");
        if (valor.signum() <= 0)
            throw FalhaApi.conflito("O valor anual deve ser positivo para aprovar.");
        if (new BigDecimal(totais(projeto).get("saldoDisponivel")).compareTo(valor) < 0)
            throw FalhaApi.conflito("Saldo disponível insuficiente para reservar o valor anual.");
        compromissoAnualRepo.criar(id, a.exercicio(), valor, SessaoAtual.id());
        contratoRepo.registrarAprovacao(SessaoAtual.id(), id);
        auditoria.registrar("APROVAR_E_RESERVAR", "contrato", id);
    }

    private String hash(Pagamento p) {
        try {
            // Comprimentos tornam a representação inequívoca mesmo com delimitadores no texto.
            var valores =
                    Arrays.asList(
                            p.compromissoId().toString(),
                            p.projetoId().toString(),
                            p.valor().setScale(2).toPlainString(),
                            p.dataPagamento().toString(),
                            p.referencia().strip(),
                            p.observacao() == null ? "" : p.observacao().strip(),
                            Long.toString(SessaoAtual.id()));
            StringBuilder canonical = new StringBuilder();
            for (String v : valores) canonical.append(v.length()).append(':').append(v);
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResultadoPagamento pagar(Pagamento p) {
        // O projeto serializa pagamentos do mesmo orçamento; a restrição única protege
        // a chave de operação entre projetos distintos.
        projetoRepo.bloquearMapa(p.projetoId());
        String hash = hash(p);
        var anteriores =
                pagamentoRepo.findByChaveOperacao(p.chaveOperacao()).stream()
                        .map(br.unisc.psagestor.financeiro.model.Pagamento::dados)
                        .toList();
        if (!anteriores.isEmpty()) {
            var anterior = anteriores.getFirst();
            if (!hash.equals(anterior.get("hash_requisicao")))
                throw FalhaApi.conflito("Chave de operação já usada com outros dados ou usuário.");
            return new ResultadoPagamento(true, saidaPagamento(anterior));
        }
        var co = compromissoAnualRepo.bloquearMapa(p.compromissoId());
        var contrato = contratoRepo.obterMapa(Dados.numero(co, "contrato_id"));
        if (Dados.numero(contrato, "projeto_id") != p.projetoId()
                || !"APROVADO".equals(contrato.get("status")))
            throw FalhaApi.conflito(
                    "O compromisso deve pertencer ao projeto e a um contrato aprovado.");
        var pago = pagamentoRepo.somaPorCompromisso(p.compromissoId());
        if (pago.add(p.valor()).compareTo(Dados.decimal(co, "valor")) > 0)
            throw FalhaApi.conflito("O pagamento excede o saldo do compromisso anual.");
        long id =
                pagamentoRepo.criar(
                        p.compromissoId(),
                        p.dataPagamento(),
                        p.valor(),
                        p.referencia().strip(),
                        p.chaveOperacao(),
                        hash,
                        p.observacao(),
                        SessaoAtual.id());
        auditoria.registrar("PAGAMENTO_SIMULADO", "pagamento", id);
        return new ResultadoPagamento(false, saidaPagamento(pagamentoRepo.obterMapa(id)));
    }

    public Map<String, Object> saidaPagamento(Map<String, Object> row) {
        var out = new LinkedHashMap<>(row);
        out.remove("hash_requisicao");
        return Dados.saida(out);
    }

    public Object consultarAporte(long id) {
        return consultas.aporte(id);
    }

    public Object listarAportes(long id, int page, int size) {
        projetoRepo.obterMapa(id);
        return consultas.aportes(id, page, size);
    }

    public Object consultarPagamento(long id) {
        var p = consultas.pagamento(id);
        SessaoAtual.verificarProdutor(
                consultas.produtorCompromisso(Dados.numero(p, "compromisso_id")));
        return saidaPagamento(p);
    }

    public Object listarPagamentos(
            Long projetoId,
            Long contratoId,
            LocalDate inicio,
            LocalDate fim,
            String chave,
            int page,
            int size) {
        if (inicio != null && fim != null && fim.isBefore(inicio))
            throw new FalhaApi(400, "PERIODO_INVALIDO", "O fim deve ser posterior ao início.");
        return consultas.pagamentos(projetoId, contratoId, inicio, fim, chave, page, size);
    }
}
