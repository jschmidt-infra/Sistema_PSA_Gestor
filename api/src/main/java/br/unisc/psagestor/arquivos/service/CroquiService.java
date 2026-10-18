package br.unisc.psagestor.arquivos.service;

import br.unisc.psagestor.acesso.service.AuditoriaService;
import br.unisc.psagestor.arquivos.repository.CroquiConsultaRepository;
import br.unisc.psagestor.arquivos.repository.CroquiRepository;
import br.unisc.psagestor.cadastros.repository.PropriedadeRepository;
import br.unisc.psagestor.cadastros.service.CadastroService;
import br.unisc.psagestor.comum.api.Dados;
import br.unisc.psagestor.comum.api.FalhaApi;
import br.unisc.psagestor.comum.api.RegraNegocioException;
import br.unisc.psagestor.comum.api.SessaoAtual;
import br.unisc.psagestor.contratos.repository.ContratoRepository;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CroquiService {
    private final ContratoRepository contratoRepo;
    private final PropriedadeRepository propriedadeRepo;
    private final CroquiRepository croquiRepo;
    private final AuditoriaService auditoria;
    private final CroquiConsultaRepository consultas;
    private final ArquivoService arquivos;
    private final CadastroService cadastros;

    public CroquiService(
            ArquivoService arquivos,
            CadastroService cadastros,
            CroquiConsultaRepository consultas,
            ContratoRepository contratoRepo,
            PropriedadeRepository propriedadeRepo,
            CroquiRepository croquiRepo,
            AuditoriaService auditoria) {
        this.contratoRepo = contratoRepo;
        this.propriedadeRepo = propriedadeRepo;
        this.croquiRepo = croquiRepo;
        this.auditoria = auditoria;
        this.consultas = consultas;
        this.arquivos = arquivos;
        this.cadastros = cadastros;
    }

    @Transactional
    public long criar(long propriedade, MultipartFile file, BigDecimal area, String observacoes) {
        var p = propriedadeRepo.bloquearMapa(propriedade);
        if (area == null
                || area.signum() <= 0
                || area.scale() > 4
                || area.compareTo(Dados.decimal(p, "area_total")) > 0)
            throw new RegraNegocioException(
                    "AREA_INVALIDA",
                    "Informe área positiva, com até quatro casas decimais, limitada à propriedade.");
        if (observacoes != null && observacoes.length() > 5000)
            throw new RegraNegocioException("OBSERVACOES_INVALIDAS", "Use até 5.000 caracteres.");
        var c = arquivos.validar(file);
        String chave = arquivos.guardar(c.bytes(), c.mime());
        long id =
                croquiRepo.criar(
                        propriedade,
                        c.nome(),
                        chave,
                        c.mime(),
                        c.bytes().length,
                        area,
                        observacoes,
                        SessaoAtual.id());
        auditoria.registrar("CRIAR", "croqui", id);
        return id;
    }

    public Map<String, Object> buscar(long id) {
        var c = croquiRepo.obterMapa(id);
        cadastros.propriedade(Dados.numero(c, "propriedade_id"));
        return c;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void editar(long id, MultipartFile file, BigDecimal area, String observacoes) {
        var anterior = croquiRepo.obterMapa(id);
        long propriedade = Dados.numero(anterior, "propriedade_id");
        propriedadeRepo.bloquearMapa(propriedade);
        croquiRepo.bloquearMapa(id);
        if (contratoRepo.quantidadeAprovadosComCroqui(id) > 0)
            throw FalhaApi.conflito(
                    "O croqui vinculado a contrato aprovado não pode ser alterado.");
        var p = propriedadeRepo.obterMapa(propriedade);
        if (area == null
                || area.signum() <= 0
                || area.scale() > 4
                || area.compareTo(Dados.decimal(p, "area_total")) > 0)
            throw new RegraNegocioException("AREA_INVALIDA", "Área inválida para a propriedade.");
        if (observacoes != null && observacoes.length() > 5000)
            throw new RegraNegocioException("OBSERVACOES_INVALIDAS", "Use até 5.000 caracteres.");
        var c = arquivos.validar(file);
        String chave = arquivos.guardar(c.bytes(), c.mime());
        arquivos.obsoleto((String) anterior.get("chave"));
        croquiRepo.substituirArquivo(
                c.nome(), chave, c.mime(), c.bytes().length, area, observacoes, id);
        auditoria.registrar("SUBSTITUIR", "croqui", id);
    }

    public record Download(String nome, String mime, byte[] conteudo) {}

    public Object listar(long id, int page, int size) {
        cadastros.propriedade(id);
        return consultas.listar(id, page, size);
    }

    public Download baixar(long id) {
        var c = buscar(id);
        return new Download(
                (String) c.get("nome_original"),
                (String) c.get("tipo_mime"),
                arquivos.abrir((String) c.get("chave")));
    }
}
