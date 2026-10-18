package br.unisc.psagestor.comum.api;

import java.util.List;

public record ErroApi(String codigo, String mensagem, List<ErroCampo> campos, String requisicaoId) {
    public record ErroCampo(String campo, String mensagem) {}

    public static ErroApi de(String codigo, String mensagem, String id) {
        return new ErroApi(codigo, mensagem, List.of(), id);
    }
}
