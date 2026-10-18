package br.unisc.psagestor.consultas.service;

import br.unisc.psagestor.cache.model.IndicadoresNaoFinanceiros;
import br.unisc.psagestor.cache.service.CacheIndicadores;
import br.unisc.psagestor.comum.api.FalhaApi;
import br.unisc.psagestor.comum.api.SessaoAtual;
import br.unisc.psagestor.consultas.repository.ConsultaRepository;
import br.unisc.psagestor.contratos.service.VencimentoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaService {
    private final ConsultaRepository repository;
    private final CacheIndicadores cache;
    private final VencimentoService vencimentos;

    public ConsultaService(
            ConsultaRepository repository, CacheIndicadores cache, VencimentoService vencimentos) {
        this.repository = repository;
        this.cache = cache;
        this.vencimentos = vencimentos;
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && fim.isBefore(inicio))
            throw new FalhaApi(400, "PERIODO_INVALIDO", "O fim deve ser posterior ao início.");
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Object relatorio(
            Long projetoId, Long subBaciaId, LocalDate inicio, LocalDate fim, int page, int size) {
        validarPeriodo(inicio, fim);
        var pagina = repository.pagamentos(projetoId, subBaciaId, inicio, fim, page, size);
        return Map.of(
                "items",
                pagina.items(),
                "page",
                page,
                "size",
                size,
                "totalItems",
                pagina.totalItems(),
                "totalPago",
                repository.totais(projetoId, subBaciaId, inicio, fim).valor().toPlainString());
    }

    public Object indicadores(
            Long projetoId,
            Long subBaciaId,
            LocalDate dataReferencia,
            LocalDate inicio,
            LocalDate fim) {
        validarPeriodo(inicio, fim);
        LocalDate data =
                dataReferencia == null
                        ? LocalDate.now(ZoneId.of("America/Sao_Paulo"))
                        : dataReferencia;
        String chave =
                "psa:indicadores:v1:"
                        + SessaoAtual.id()
                        + ":GESTOR:"
                        + projetoId
                        + ":"
                        + subBaciaId
                        + ":"
                        + data;
        var naoFinanceiros =
                cache.consultar(chave)
                        .orElseGet(
                                () -> {
                                    var area = repository.area(projetoId, subBaciaId, data);
                                    var result =
                                            new IndicadoresNaoFinanceiros(
                                                    area.produtores(), area.area(), Instant.now());
                                    cache.guardar(chave, result);
                                    return result;
                                });
        var valores = repository.totais(projetoId, subBaciaId, inicio, fim);
        var out = new LinkedHashMap<String, Object>();
        out.put("produtoresParticipantes", naoFinanceiros.produtoresParticipantes());
        out.put("areaProtegida", naoFinanceiros.areaProtegida().toPlainString());
        out.put("apuradoEm", naoFinanceiros.apuradoEm());
        out.put("totalPago", valores.valor().toPlainString());
        out.put(
                "mediaPorPagamento",
                valores.quantidade() == 0
                        ? null
                        : valores.valor()
                                .divide(
                                        BigDecimal.valueOf(valores.quantidade()),
                                        2,
                                        RoundingMode.HALF_UP)
                                .toPlainString());
        out.put("financeiroApuradoEm", Instant.now());
        return out;
    }

    public Object historico(String situacaoVencimento, int page, int size) {
        var contexto = vencimentos.contexto();
        return contexto.enriquecer(
                repository.historico(
                        SessaoAtual.usuario().produtorId(),
                        contexto.filtro(situacaoVencimento),
                        page,
                        size));
    }

    public Object extrato(int page, int size) {
        return repository.extrato(SessaoAtual.usuario().produtorId(), page, size);
    }
}
