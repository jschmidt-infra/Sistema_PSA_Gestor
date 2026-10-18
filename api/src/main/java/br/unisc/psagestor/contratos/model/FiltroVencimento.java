package br.unisc.psagestor.contratos.model;

import java.time.LocalDate;

/** Intervalo inclusivo aplicado no banco antes da contagem e paginação. */
public record FiltroVencimento(LocalDate de, LocalDate ate) {}
