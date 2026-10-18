package br.unisc.psagestor.calculo.service;

import static org.assertj.core.api.Assertions.*;

import br.unisc.psagestor.comum.api.RegraNegocioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculadoraPsaTest {
    private final CalculadoraPsa calculadora = new CalculadoraPsa();

    private BigDecimal d(String x) {
        return new BigDecimal(x);
    }

    @Test
    void aplicaFormulaDocumentadaComDadosFicticios() {
        assertThat(calculadora.calcular(d("10000"), d("0.04"), d("0.8"), d("2.5"), d("10")))
                .isEqualTo(d("800.00"));
    }

    @Test
    void arredondaSomenteOResultadoFinal() {
        assertThat(calculadora.calcular(d("1"), d("0.5"), d("1"), d("1.01"), d("2")))
                .isEqualTo(d("0.51"));
    }

    @Test
    void rejeitaAreaAcimaDaPropriedade() {
        assertThatThrownBy(
                        () ->
                                calculadora.calcular(
                                        d("10000"), d("0.04"), d("0.8"), d("11"), d("10")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("área total");
    }

    @Test
    void naoAceitaPercentualInteiroComoSeFosseFracao() {
        assertThatThrownBy(
                        () -> calculadora.calcular(d("10000"), d("4"), d("0.8"), d("2"), d("10")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void naoInventaParametroAusente() {
        assertThatThrownBy(() -> calculadora.calcular(d("10000"), null, d("0.8"), d("2"), d("10")))
                .isInstanceOf(RegraNegocioException.class);
    }
}
