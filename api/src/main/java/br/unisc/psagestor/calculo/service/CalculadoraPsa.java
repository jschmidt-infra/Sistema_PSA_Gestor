package br.unisc.psagestor.calculo.service;

import br.unisc.psagestor.comum.api.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/** Fórmula única. Não escolhe percentual, questionário ou pesos pelo programa. */
@Service
public class CalculadoraPsa {
    public BigDecimal calcular(
            BigDecimal valorHectare,
            BigDecimal percentual,
            BigDecimal pontuacao,
            BigDecimal areaContratada,
            BigDecimal areaTotal) {
        positivo(valorHectare, "valorHectare");
        positivo(areaContratada, "areaContratada");
        positivo(areaTotal, "areaTotal");
        fracao(percentual, "percentual");
        fracao(pontuacao, "pontuacao");
        if (areaContratada.compareTo(areaTotal) > 0) {
            throw new RegraNegocioException(
                    "AREA_INVALIDA", "A área contratada não pode exceder a área total.");
        }
        var valor =
                valorHectare
                        .multiply(percentual)
                        .multiply(pontuacao)
                        .multiply(areaContratada)
                        .setScale(2, RoundingMode.HALF_UP);
        if (valor.precision() > 15)
            throw new RegraNegocioException(
                    "VALOR_INVALIDO", "Valor anual excede o limite de armazenamento.");
        return valor;
    }

    private void positivo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() <= 0)
            throw new RegraNegocioException(
                    "PARAMETRO_INVALIDO", campo + " deve ser maior que zero.");
    }

    private void fracao(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() < 0 || valor.compareTo(BigDecimal.ONE) > 0) {
            throw new RegraNegocioException(
                    "PARAMETRO_INVALIDO", campo + " deve ser uma fração entre 0 e 1.");
        }
    }
}
