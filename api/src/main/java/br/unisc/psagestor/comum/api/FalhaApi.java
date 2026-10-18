package br.unisc.psagestor.comum.api;

public class FalhaApi extends RuntimeException {
    private final int status;
    private final String codigo;

    public FalhaApi(int status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public int status() {
        return status;
    }

    public String codigo() {
        return codigo;
    }

    public static FalhaApi inexistente() {
        return new FalhaApi(404, "NAO_ENCONTRADO", "Registro não encontrado.");
    }

    public static FalhaApi conflito(String mensagem) {
        return new FalhaApi(409, "CONFLITO", mensagem);
    }
}
