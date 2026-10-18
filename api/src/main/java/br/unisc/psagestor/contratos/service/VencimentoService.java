package br.unisc.psagestor.contratos.service;

import br.unisc.psagestor.comum.api.FalhaApi;
import br.unisc.psagestor.comum.api.Pagina;
import br.unisc.psagestor.configuracao.service.ConfiguracaoVencimentoService;
import br.unisc.psagestor.contratos.model.FiltroVencimento;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VencimentoService {
    private final ConfiguracaoVencimentoService configuracao;
    private final Clock relogio;

    @Autowired
    public VencimentoService(ConfiguracaoVencimentoService configuracao) {
        this(configuracao, Clock.system(ZoneId.of("America/Sao_Paulo")));
    }

    VencimentoService(ConfiguracaoVencimentoService configuracao, Clock relogio) {
        this.configuracao = configuracao;
        this.relogio = relogio.withZone(ZoneId.of("America/Sao_Paulo"));
    }

    public Contexto contexto() {
        return new Contexto(LocalDate.now(relogio), configuracao.consultar().antecedenciaDias());
    }

    /**
     * Uma única data e configuração por resposta, mesmo durante virada de dia ou alteração
     * administrativa.
     */
    public record Contexto(LocalDate hoje, int antecedenciaDias) {
        public FiltroVencimento filtro(String situacao) {
            if (situacao == null) return null;
            return switch (situacao) {
                case "VENCIDO" -> new FiltroVencimento(null, hoje.minusDays(1));
                case "VENCE_HOJE" -> new FiltroVencimento(hoje, hoje);
                case "PROXIMO_DO_VENCIMENTO" ->
                        new FiltroVencimento(hoje.plusDays(1), hoje.plusDays(antecedenciaDias));
                case "DENTRO_DO_PRAZO" ->
                        new FiltroVencimento(hoje.plusDays(antecedenciaDias + 1L), null);
                default ->
                        throw new FalhaApi(
                                400,
                                "SITUACAO_VENCIMENTO_INVALIDA",
                                "Situação de vencimento inválida.");
            };
        }

        public Map<String, Object> enriquecer(Map<String, Object> contrato) {
            var saida = new LinkedHashMap<>(contrato);
            LocalDate fim = LocalDate.parse(contrato.get("fim").toString());
            long dias = ChronoUnit.DAYS.between(hoje, fim);
            boolean aprovado = "APROVADO".equals(contrato.get("status"));
            String situacao = classificar(aprovado, dias);
            saida.put("situacaoVencimento", situacao);
            saida.put("diasParaVencimento", aprovado ? dias : null);
            saida.put("antecedenciaAvisoDias", antecedenciaDias);
            saida.put("dataReferenciaVencimento", hoje);
            return saida;
        }

        private String classificar(boolean aprovado, long dias) {
            if (!aprovado) return "NAO_APLICAVEL";
            if (dias < 0) return "VENCIDO";
            if (dias == 0) return "VENCE_HOJE";
            if (dias <= antecedenciaDias) return "PROXIMO_DO_VENCIMENTO";
            return "DENTRO_DO_PRAZO";
        }

        public Pagina<Map<String, Object>> enriquecer(Pagina<Map<String, Object>> pagina) {
            return new Pagina<>(
                    pagina.items().stream().map(this::enriquecer).toList(),
                    pagina.page(),
                    pagina.size(),
                    pagina.totalItems());
        }
    }
}
