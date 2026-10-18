package br.unisc.psagestor.contratos.service;

import br.unisc.psagestor.acesso.repository.UsuarioRepository;
import br.unisc.psagestor.acesso.service.AuditoriaService;
import br.unisc.psagestor.arquivos.repository.CroquiRepository;
import br.unisc.psagestor.arquivos.service.ArquivoService;
import br.unisc.psagestor.cadastros.repository.LocalidadeRepository;
import br.unisc.psagestor.cadastros.repository.ProdutorRepository;
import br.unisc.psagestor.cadastros.repository.PropriedadeRepository;
import br.unisc.psagestor.cadastros.repository.SubBaciaRepository;
import br.unisc.psagestor.calculo.repository.CalculoPsaRepository;
import br.unisc.psagestor.comum.api.Dados;
import br.unisc.psagestor.comum.api.FalhaApi;
import br.unisc.psagestor.comum.api.RegraNegocioException;
import br.unisc.psagestor.comum.api.SessaoAtual;
import br.unisc.psagestor.configuracao.repository.ProjetoRepository;
import br.unisc.psagestor.contratos.repository.ContratoConsultaRepository;
import br.unisc.psagestor.contratos.repository.ContratoRepository;
import br.unisc.psagestor.diagnostico.repository.DiagnosticoRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ContratoService {
    private final UsuarioRepository usuarioRepo;
    private final SubBaciaRepository subBaciaRepo;
    private final PropriedadeRepository propriedadeRepo;
    private final ProjetoRepository projetoRepo;
    private final ProdutorRepository produtorRepo;
    private final LocalidadeRepository localidadeRepo;
    private final DiagnosticoRepository diagnosticoRepo;
    private final CroquiRepository croquiRepo;
    private final ContratoRepository contratoRepo;
    private final CalculoPsaRepository calculoPsaRepo;
    private final AuditoriaService auditoria;
    private final ContratoConsultaRepository consultas;
    private final ArquivoService arquivos;
    private final JsonMapper json;
    private final GeradorContratoPdf gerador;
    private final VencimentoService vencimentos;

    public ContratoService(
            ArquivoService arquivos,
            JsonMapper json,
            GeradorContratoPdf gerador,
            ContratoConsultaRepository consultas,
            UsuarioRepository usuarioRepo,
            SubBaciaRepository subBaciaRepo,
            PropriedadeRepository propriedadeRepo,
            ProjetoRepository projetoRepo,
            ProdutorRepository produtorRepo,
            LocalidadeRepository localidadeRepo,
            DiagnosticoRepository diagnosticoRepo,
            CroquiRepository croquiRepo,
            ContratoRepository contratoRepo,
            CalculoPsaRepository calculoPsaRepo,
            AuditoriaService auditoria,
            VencimentoService vencimentos) {
        this.vencimentos = vencimentos;
        this.usuarioRepo = usuarioRepo;
        this.subBaciaRepo = subBaciaRepo;
        this.propriedadeRepo = propriedadeRepo;
        this.projetoRepo = projetoRepo;
        this.produtorRepo = produtorRepo;
        this.localidadeRepo = localidadeRepo;
        this.diagnosticoRepo = diagnosticoRepo;
        this.croquiRepo = croquiRepo;
        this.contratoRepo = contratoRepo;
        this.calculoPsaRepo = calculoPsaRepo;
        this.auditoria = auditoria;
        this.consultas = consultas;
        this.arquivos = arquivos;
        this.json = json;
        this.gerador = gerador;
    }

    public record Entrada(
            @NotNull @Positive Long projetoId,
            @NotNull @Positive Long calculoId,
            @Positive Long croquiId,
            @NotNull LocalDate inicio,
            @NotNull LocalDate fim) {}

    public record Assinatura(@NotNull @PastOrPresent LocalDate dataRegistro) {}

    public Map<String, Object> buscar(long id) {
        var c = contratoRepo.obterMapa(id);
        SessaoAtual.verificarProdutor(Dados.numero(c, "produtor_id"));
        return c;
    }

    public Map<String, Object> saida(long id) {
        var c = new LinkedHashMap<>(buscar(id));
        c.put("documento_disponivel", c.get("documento_chave") != null);
        c.remove("documento_chave");
        c.remove("documento_snapshot");
        return vencimentos.contexto().enriquecer(Dados.saida(c));
    }

    public void rascunho(Map<String, Object> c) {
        if (!"RASCUNHO".equals(c.get("status")))
            throw FalhaApi.conflito("O contrato aprovado não pode ser alterado.");
    }

    @Transactional
    public long salvar(Long id, Entrada e) {
        projetoRepo.bloquearMapa(e.projetoId());
        if (e.fim().isBefore(e.inicio()))
            throw new RegraNegocioException(
                    "VIGENCIA_INVALIDA", "O fim deve ser igual ou posterior ao início.");
        var calc = calculoPsaRepo.obterMapa(e.calculoId());
        var d = diagnosticoRepo.obterMapa(Dados.numero(calc, "diagnostico_id"));
        var p = propriedadeRepo.obterMapa(Dados.numero(d, "propriedade_id"));
        if (Dados.numero(d, "projeto_id") != e.projetoId())
            throw FalhaApi.conflito("O cálculo pertence a outro projeto.");
        if (!"CONCLUIDO".equals(d.get("status"))
                || Dados.numero(d, "revisao") != Dados.numero(calc, "revisao_diagnostico"))
            throw FalhaApi.conflito("Use um cálculo da revisão concluída do diagnóstico.");
        if (e.croquiId() != null)
            validarCroqui(
                    e.croquiId(), Dados.numero(p, "id"), Dados.decimal(calc, "area_contratada"));
        if (id == null)
            id =
                    contratoRepo.criar(
                            "PSA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24),
                            ((Number) p.get("produtor_id")).longValue(),
                            ((Number) p.get("id")).longValue(),
                            e.projetoId(),
                            e.calculoId(),
                            e.croquiId(),
                            e.inicio(),
                            e.fim(),
                            SessaoAtual.id());
        else {
            var c = contratoRepo.bloquearMapa(id);
            rascunho(c);
            if (Dados.numero(c, "projeto_id") != e.projetoId()
                    || Dados.numero(c, "propriedade_id") != Dados.numero(p, "id"))
                throw FalhaApi.conflito(
                        "O projeto e a propriedade do contrato não podem ser alterados.");
            arquivos.obsoleto((String) c.get("documento_chave"));
            contratoRepo.atualizarRascunho(e.calculoId(), e.croquiId(), e.inicio(), e.fim(), id);
        }
        auditoria.registrar("SALVAR", "contrato", id);
        return id;
    }

    private void validarCroqui(long id, long propriedade, BigDecimal area) {
        var c = croquiRepo.obterMapa(id);
        if (Dados.numero(c, "propriedade_id") != propriedade
                || Dados.decimal(c, "area_psa").compareTo(area) != 0)
            throw FalhaApi.conflito(
                    "O croqui deve pertencer à propriedade e ter a mesma área contratada.");
    }

    public String snapshot(Map<String, Object> c) {
        var produtor = produtorRepo.obterMapa(Dados.numero(c, "produtor_id"));
        if (produtor.get("documento") == null
                || produtor.get("endereco") == null
                || produtor.get("endereco").toString().isBlank())
            throw new RegraNegocioException(
                    "CADASTRO_INCOMPLETO",
                    "Complete o documento e o endereço do produtor antes de formalizar.");
        var calc = calculoPsaRepo.obterMapa(Dados.numero(c, "calculo_id"));
        var d = diagnosticoRepo.obterMapa(Dados.numero(calc, "diagnostico_id"));
        if (!"CONCLUIDO".equals(d.get("status"))
                || Dados.numero(d, "revisao") != Dados.numero(calc, "revisao_diagnostico"))
            throw FalhaApi.conflito("O diagnóstico mudou; gere um cálculo atualizado.");
        if (c.get("croqui_id") == null)
            throw new RegraNegocioException("CROQUI_OBRIGATORIO", "Vincule um croqui ao contrato.");
        validarCroqui(
                Dados.numero(c, "croqui_id"),
                Dados.numero(c, "propriedade_id"),
                Dados.decimal(calc, "area_contratada"));
        var dados = new TreeMap<String, Object>();
        dados.put("numero", c.get("numero"));
        dados.put("inicio", c.get("inicio").toString());
        dados.put("fim", c.get("fim").toString());
        dados.put("modelo", "PRELIMINAR-1");
        dados.put("produtor", new TreeMap<>(Dados.saida(produtor)));
        dados.put(
                "propriedade",
                new TreeMap<>(
                        Dados.saida(propriedadeRepo.obterMapa(Dados.numero(c, "propriedade_id")))));
        dados.put("calculo", new TreeMap<>(Dados.saida(calc)));
        dados.put("croquiId", c.get("croqui_id"));
        dados.put("projetoId", c.get("projeto_id"));
        dados.put(
                "croqui",
                new TreeMap<>(Dados.saida(croquiRepo.obterMapa(Dados.numero(c, "croqui_id")))));
        var propriedade = propriedadeRepo.obterMapa(Dados.numero(c, "propriedade_id"));
        dados.put(
                "localidade",
                new TreeMap<>(
                        Dados.saida(
                                localidadeRepo.obterMapa(
                                        Dados.numero(propriedade, "localidade_id")))));
        dados.put(
                "projeto",
                new TreeMap<>(Dados.saida(projetoRepo.obterMapa(Dados.numero(c, "projeto_id")))));
        dados.put("tecnico", usuarioRepo.obterMapa(Dados.numero(d, "tecnico_id")).get("nome"));
        dados.put("dataVisita", d.get("data_visita").toString());
        if (propriedade.get("sub_bacia_id") != null)
            dados.put(
                    "subBacia",
                    new TreeMap<>(
                            Dados.saida(
                                    subBaciaRepo.obterMapa(
                                            Dados.numero(propriedade, "sub_bacia_id")))));
        return json.writeValueAsString(dados);
    }

    @Transactional
    public void gerar(long id) {
        var c = contratoRepo.bloquearMapa(id);
        rascunho(c);
        String snapshot = snapshot(c);
        var produtor = produtorRepo.obterMapa(Dados.numero(c, "produtor_id"));
        var calc = calculoPsaRepo.obterMapa(Dados.numero(c, "calculo_id"));
        var propriedade = propriedadeRepo.obterMapa(Dados.numero(c, "propriedade_id"));
        var localidade = localidadeRepo.obterMapa(Dados.numero(propriedade, "localidade_id"));
        var projeto = projetoRepo.obterMapa(Dados.numero(c, "projeto_id"));
        var diagnostico = diagnosticoRepo.obterMapa(Dados.numero(calc, "diagnostico_id"));
        var tecnico = usuarioRepo.obterMapa(Dados.numero(diagnostico, "tecnico_id"));
        String subBacia =
                propriedade.get("sub_bacia_id") == null
                        ? "Não informada"
                        : subBaciaRepo
                                .obterMapa(Dados.numero(propriedade, "sub_bacia_id"))
                                .get("nome")
                                .toString();
        List<String> linhas =
                List.of(
                        "PSA GESTOR — CONTRATO PRELIMINAR DE PAGAMENTO POR SERVIÇOS AMBIENTAIS",
                        "Contrato "
                                + c.get("numero")
                                + " | Projeto: "
                                + projeto.get("nome")
                                + " ("
                                + c.get("projeto_id")
                                + "). Modelo PRELIMINAR-1 para demonstração acadêmica.",
                        "IDENTIFICAÇÃO DAS PARTES",
                        "Contratante / gestora do programa: [instituição, identificação, endereço e representante a definir pelo responsável pelo programa].",
                        "Produtor contratado: "
                                + produtor.get("nome")
                                + ". CPF/CNPJ: "
                                + produtor.get("documento")
                                + ". Endereço: "
                                + produtor.get("endereco")
                                + ". Telefone: "
                                + Objects.toString(produtor.get("telefone"), "Não informado")
                                + ". E-mail: "
                                + Objects.toString(produtor.get("email"), "Não informado")
                                + ".",
                        "CLÁUSULA PRIMEIRA — OBJETO E ÁREA CONTRATADA",
                        "Registro de incentivo por serviços ambientais voltados à conservação da vegetação nativa, dos recursos hídricos e das práticas conservacionistas na propriedade "
                                + propriedade.get("nome")
                                + " ("
                                + c.get("propriedade_id")
                                + "). Localidade: "
                                + localidade.get("nome")
                                + ", "
                                + localidade.get("municipio")
                                + "/"
                                + localidade.get("uf")
                                + ". Sub-bacia: "
                                + subBacia
                                + ".",
                        "Área total: "
                                + propriedade.get("area_total")
                                + " ha. Área contratada: "
                                + calc.get("area_contratada")
                                + " ha. Croqui: "
                                + c.get("croqui_id")
                                + ". CAR: "
                                + propriedade.get("car_situacao")
                                + "; número: "
                                + Objects.toString(propriedade.get("car_numero"), "Não informado")
                                + ".",
                        "CLÁUSULA SEGUNDA — DIAGNÓSTICO AMBIENTAL",
                        "Diagnóstico "
                                + calc.get("diagnostico_id")
                                + ", revisão "
                                + calc.get("revisao_diagnostico")
                                + ", aplicado por "
                                + tecnico.get("nome")
                                + " em "
                                + diagnostico.get("data_visita")
                                + ". Pontuação: "
                                + calc.get("pontuacao")
                                + ". Cálculo associado: "
                                + calc.get("id")
                                + ". Alterações posteriores não modificam automaticamente os valores deste instrumento.",
                        "CLÁUSULA TERCEIRA — VALOR ANUAL E APURAÇÃO",
                        "Valor anual = (valor da terra por hectare × percentual) × pontuação × área contratada. Valor da terra: R$ "
                                + calc.get("valor_hectare")
                                + "/ha; percentual (fração): "
                                + calc.get("percentual")
                                + "; pontuação: "
                                + calc.get("pontuacao")
                                + "; área: "
                                + calc.get("area_contratada")
                                + " ha. Valor anual: R$ "
                                + calc.get("valor_anual")
                                + ". O cálculo de demonstração não representa metodologia oficial aprovada.",
                        "CLÁUSULA QUARTA — VIGÊNCIA",
                        "Vigência de "
                                + c.get("inicio")
                                + " a "
                                + c.get("fim")
                                + ". Renovação, exercícios posteriores e eventuais condições adicionais dependem da definição do programa.",
                        "CLÁUSULA QUINTA — REGISTRO DOS PAGAMENTOS",
                        "O valor anual pode ser registrado integralmente ou em repasses parciais, respeitando o saldo do compromisso. Cada registro identifica valor, data, contrato, produtor e projeto. A aprovação reserva o compromisso anual inicial. Os registros são simulados: o sistema não executa transferências bancárias nem comprova pagamentos externos.",
                        "Condições específicas de conservação, acompanhamento e liberação: [a definir pelo responsável]. Não se presume bloqueio automático por vistoria enquanto essa regra não estiver definida.",
                        "FORMALIZAÇÃO",
                        "Local, data e representante: [preencher após validação das partes]. A assinatura é registrada separadamente, de forma simulada, sem representar assinatura digital com validade jurídica. Este instrumento permanece preliminar até a validação do texto pelo responsável pelo programa.");
        byte[] pdf = gerador.gerar(linhas);
        String chave = arquivos.guardar(pdf, "application/pdf");
        arquivos.obsoleto((String) c.get("documento_chave"));
        contratoRepo.registrarDocumento(chave, snapshot, id);
        auditoria.registrar("GERAR_DOCUMENTO", "contrato", id);
    }

    @Transactional
    public void assinar(long id, Assinatura a) {
        var c = contratoRepo.bloquearMapa(id);
        if (c.get("documento_chave") == null)
            throw FalhaApi.conflito("Gere o documento antes de registrar a assinatura.");
        if (c.get("assinatura_em") != null) throw FalhaApi.conflito("Assinatura já registrada.");
        contratoRepo.registrarAssinatura(a.dataRegistro(), id);
        auditoria.registrar("ASSINATURA_SIMULADA", "contrato", id);
    }

    public Object listar(
            Long projetoId,
            String status,
            LocalDate vencimentoAte,
            String situacaoVencimento,
            int page,
            int size) {
        if (status != null && !Set.of("RASCUNHO", "APROVADO").contains(status))
            throw new FalhaApi(400, "STATUS_INVALIDO", "Status inválido.");
        var contexto = vencimentos.contexto();
        return contexto.enriquecer(
                consultas.listar(
                        projetoId,
                        status,
                        vencimentoAte,
                        contexto.filtro(situacaoVencimento),
                        page,
                        size));
    }

    public Object compromissos(long id, int page, int size) {
        buscar(id);
        return consultas.compromissos(id, page, size);
    }

    public byte[] documento(long id) {
        var c = buscar(id);
        if (c.get("documento_chave") == null) throw FalhaApi.inexistente();
        return arquivos.abrir((String) c.get("documento_chave"));
    }
}
