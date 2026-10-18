package br.unisc.psagestor.arquivos.service;

import java.io.InputStream;

/**
 * Porta de armazenamento privado; a autorização e a validação pertencem aos serviços de aplicação.
 */
public interface ArquivoStorage extends AutoCloseable {
    void guardar(String chave, InputStream conteudo, long tamanho, String tipoMime);

    /** O chamador deve fechar o fluxo retornado após a leitura. */
    InputStream abrir(String chave);

    void removerObjetoNovo(String chave);

    /** Adaptadores sem recursos persistentes não precisam executar nenhuma ação ao encerrar. */
    @Override
    default void close() {}
}
