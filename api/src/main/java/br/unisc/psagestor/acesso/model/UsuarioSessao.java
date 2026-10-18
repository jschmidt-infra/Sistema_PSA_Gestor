package br.unisc.psagestor.acesso.model;

import java.io.Serializable;

/** Identidade da sessão sem senha ou hash de senha. */
public record UsuarioSessao(
        Long id, String nome, String email, Perfil perfil, Long produtorId, long versaoCredenciais)
        implements Serializable {
    public static UsuarioSessao de(Usuario usuario) {
        return new UsuarioSessao(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getProdutorId(),
                usuario.getVersaoCredenciais());
    }
}
