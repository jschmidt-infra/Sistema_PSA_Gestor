package br.unisc.psagestor.comum.api;

import java.util.List;

public record Pagina<T>(List<T> items, int page, int size, long totalItems) {
    public static void validar(int page, int size) {
        if (page < 0 || size < 1 || size > 100 || page > 1000000)
            throw new FalhaApi(
                    400, "PAGINACAO_INVALIDA", "Use page não negativo e size entre 1 e 100.");
    }
}
