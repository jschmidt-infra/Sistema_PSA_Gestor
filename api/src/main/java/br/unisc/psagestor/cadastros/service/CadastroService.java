package br.unisc.psagestor.cadastros.service;

import br.unisc.psagestor.acesso.service.AuditoriaService;
import br.unisc.psagestor.cadastros.api.CadastroEntradas.CarSituacao;
import br.unisc.psagestor.cadastros.api.CadastroEntradas.Fonte;
import br.unisc.psagestor.cadastros.api.CadastroEntradas.Levantamento;
import br.unisc.psagestor.cadastros.api.CadastroEntradas.Produtor;
import br.unisc.psagestor.cadastros.api.CadastroEntradas.Propriedade;
import br.unisc.psagestor.cadastros.api.CadastroEntradas.SituacaoFontes;
import br.unisc.psagestor.cadastros.api.CadastroEntradas.Visita;
import br.unisc.psagestor.cadastros.repository.CadastroConsultaRepository;
import br.unisc.psagestor.cadastros.repository.FonteHidricaRepository;
import br.unisc.psagestor.cadastros.repository.LocalidadeRepository;
import br.unisc.psagestor.cadastros.repository.ProdutorRepository;
import br.unisc.psagestor.cadastros.repository.PropriedadeRepository;
import br.unisc.psagestor.cadastros.repository.SubBaciaRepository;
import br.unisc.psagestor.cadastros.repository.VisitaRepository;
import br.unisc.psagestor.calculo.repository.CalculoPsaRepository;
import br.unisc.psagestor.comum.api.Dados;
import br.unisc.psagestor.comum.api.FalhaApi;
import br.unisc.psagestor.comum.api.RegraNegocioException;
import br.unisc.psagestor.comum.api.SessaoAtual;
import br.unisc.psagestor.contratos.repository.ContratoRepository;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastroService {
    private final CalculoPsaRepository calculoPsaRepo;
    private final ContratoRepository contratoRepo;
    private final VisitaRepository visitaRepo;
    private final SubBaciaRepository subBaciaRepo;
    private final PropriedadeRepository propriedadeRepo;
    private final ProdutorRepository produtorRepo;
    private final LocalidadeRepository localidadeRepo;
    private final FonteHidricaRepository fonteHidricaRepo;
    private final AuditoriaService auditoria;
    private final CadastroConsultaRepository consultas;

    public CadastroService(
            CadastroConsultaRepository consultas,
            CalculoPsaRepository calculoPsaRepo,
            ContratoRepository contratoRepo,
            VisitaRepository visitaRepo,
            SubBaciaRepository subBaciaRepo,
            PropriedadeRepository propriedadeRepo,
            ProdutorRepository produtorRepo,
            LocalidadeRepository localidadeRepo,
            FonteHidricaRepository fonteHidricaRepo,
            AuditoriaService auditoria) {
        this.calculoPsaRepo = calculoPsaRepo;
        this.contratoRepo = contratoRepo;
        this.visitaRepo = visitaRepo;
        this.subBaciaRepo = subBaciaRepo;
        this.propriedadeRepo = propriedadeRepo;
        this.produtorRepo = produtorRepo;
        this.localidadeRepo = localidadeRepo;
        this.fonteHidricaRepo = fonteHidricaRepo;
        this.auditoria = auditoria;
        this.consultas = consultas;
    }

    public Map<String, Object> propriedade(long id) {
        var p = propriedadeRepo.obterMapa(id);
        SessaoAtual.verificarProdutor(Dados.numero(p, "produtor_id"));
        return p;
    }

    @Transactional
    public long produtor(Long id, Produtor p) {
        if ((p.telefone() == null || p.telefone().isBlank())
                && (p.email() == null || p.email().isBlank()))
            throw new RegraNegocioException("CONTATO_OBRIGATORIO", "Informe telefone ou e-mail.");
        if (id == null)
            id =
                    produtorRepo.criar(
                            p.nome(),
                            p.documento(),
                            p.endereco(),
                            p.telefone(),
                            p.email(),
                            SessaoAtual.id());
        else {
            produtorRepo.bloquearMapa(id);
            produtorRepo.atualizarCadastro(
                    p.nome(),
                    p.documento(),
                    p.endereco(),
                    p.telefone(),
                    p.email(),
                    SessaoAtual.id(),
                    id);
        }
        auditoria.registrar("SALVAR", "produtor", id);
        return id;
    }

    @Transactional
    public long propriedade(Long id, Propriedade p) {
        produtorRepo.obterMapa(p.produtorId());
        localidadeRepo.obterMapa(p.localidadeId());
        if (p.subBaciaId() != null) subBaciaRepo.obterMapa(p.subBaciaId());
        if (p.carSituacao() == CarSituacao.POSSUI
                && (p.carNumero() == null || p.carNumero().isBlank()))
            throw new RegraNegocioException("CAR_OBRIGATORIO", "Informe o número do CAR.");
        String car = p.carSituacao() == CarSituacao.POSSUI ? p.carNumero() : null;
        if (id == null)
            id =
                    propriedadeRepo.criar(
                            p.produtorId(),
                            p.localidadeId(),
                            p.subBaciaId(),
                            p.nome(),
                            p.areaTotal(),
                            p.carSituacao().name(),
                            car,
                            SessaoAtual.id());
        else {
            var atual = propriedadeRepo.bloquearMapa(id);
            if (Dados.numero(atual, "produtor_id") != p.produtorId())
                throw FalhaApi.conflito(
                        "Não é permitido transferir a propriedade entre produtores.");
            if ((!Objects.equals(atual.get("localidade_id"), p.localidadeId())
                            || !Objects.equals(atual.get("sub_bacia_id"), p.subBaciaId()))
                    && contratoRepo.quantidadeAprovadosNaPropriedade(id) > 0)
                throw FalhaApi.conflito(
                        "Localidade e sub-bacia estão vinculadas ao histórico de contratos aprovados.");
            var maior = calculoPsaRepo.maiorAreaNaPropriedade(id);
            if (maior != null && p.areaTotal().compareTo(maior) < 0)
                throw FalhaApi.conflito(
                        "A área total não pode ser inferior à área dos cálculos existentes.");
            propriedadeRepo.atualizarCadastro(
                    p.localidadeId(),
                    p.subBaciaId(),
                    p.nome(),
                    p.areaTotal(),
                    p.carSituacao().name(),
                    car,
                    SessaoAtual.id(),
                    id);
        }
        auditoria.registrar("SALVAR", "propriedade", id);
        return id;
    }

    @Transactional
    public long fonte(long propriedadeId, Long id, Fonte f) {
        propriedadeRepo.bloquearMapa(propriedadeId);
        if (id == null)
            id =
                    fonteHidricaRepo.criar(
                            propriedadeId,
                            f.tipo().name(),
                            f.protecao().name(),
                            f.ativo(),
                            f.observacoes(),
                            SessaoAtual.id());
        else
            fonteHidricaRepo.atualizarFonte(
                    f.tipo().name(),
                    f.protecao().name(),
                    f.ativo(),
                    f.observacoes(),
                    SessaoAtual.id(),
                    id);
        long ativas = fonteHidricaRepo.quantidadeAtivas(propriedadeId);
        propriedadeRepo.alterarFontesSituacao(
                ativas > 0 ? "PRESENTES" : "NAO_AVALIADO", propriedadeId);
        auditoria.registrar("SALVAR", "fonte_hidrica", id);
        return id;
    }

    @Transactional
    public void levantamento(long id, Levantamento l) {
        propriedadeRepo.bloquearMapa(id);
        long ativas = fonteHidricaRepo.quantidadeAtivas(id);
        if ((l.situacao() == SituacaoFontes.AUSENTES && ativas > 0)
                || (l.situacao() == SituacaoFontes.PRESENTES && ativas == 0))
            throw FalhaApi.conflito("A situação deve corresponder às fontes ativas cadastradas.");
        propriedadeRepo.alterarFontesSituacao(l.situacao().name(), SessaoAtual.id(), id);
        auditoria.registrar("LEVANTAMENTO", "propriedade", id);
    }

    @Transactional
    public long visita(long propriedadeId, Long id, Visita v) {
        propriedade(propriedadeId);
        if (id == null)
            id =
                    visitaRepo.criar(
                            propriedadeId,
                            SessaoAtual.id(),
                            v.dataVisita(),
                            v.observacoes(),
                            SessaoAtual.id());
        else visitaRepo.atualizarVisita(v.dataVisita(), v.observacoes(), SessaoAtual.id(), id);
        auditoria.registrar("SALVAR", "visita", id);
        return id;
    }

    public Object listarProdutores(int page, int size) {
        return consultas.produtores(page, size);
    }

    public Object consultarProdutor(long id) {
        SessaoAtual.verificarProdutor(id);
        return consultas.produtor(id);
    }

    public Object consultarPropriedade(long id) {
        return Dados.saida(propriedade(id));
    }

    public Object listarPropriedades(Long produtor, int page, int size) {
        return consultas.propriedades(produtor, page, size);
    }

    public Object listarFontes(long id, int page, int size) {
        propriedade(id);
        return consultas.fontes(id, page, size);
    }

    public Object listarVisitas(long id, int page, int size) {
        propriedade(id);
        return consultas.visitas(id, page, size);
    }

    public Object localidades(int page, int size) {
        return consultas.localidades(page, size);
    }

    public Object subBacias(int page, int size) {
        return consultas.subBacias(page, size);
    }

    public Object projetos(int page, int size) {
        return consultas.projetos(page, size);
    }

    @Transactional
    public Object editarFonte(long id, Fonte entrada) {
        fonte(Dados.numero(fonteHidricaRepo.obterMapa(id), "propriedade_id"), id, entrada);
        return Dados.saida(fonteHidricaRepo.obterMapa(id));
    }

    @Transactional
    public Object editarVisita(long id, Visita entrada) {
        visita(Dados.numero(visitaRepo.obterMapa(id), "propriedade_id"), id, entrada);
        return Dados.saida(visitaRepo.obterMapa(id));
    }
}
