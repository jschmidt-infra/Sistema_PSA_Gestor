package br.unisc.psagestor.cache.model;

import java.math.BigDecimal;
import java.time.Instant;

/** Somente estes indicadores são elegíveis ao Redis; valores financeiros ficam fora. */
public record IndicadoresNaoFinanceiros(
        long produtoresParticipantes, BigDecimal areaProtegida, Instant apuradoEm) {}
