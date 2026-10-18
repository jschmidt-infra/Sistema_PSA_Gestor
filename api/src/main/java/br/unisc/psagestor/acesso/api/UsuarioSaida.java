package br.unisc.psagestor.acesso.api;

import br.unisc.psagestor.acesso.model.Perfil;
import br.unisc.psagestor.acesso.model.UsuarioSessao;

public record UsuarioSaida(
        Long id, String nome, String email, Perfil perfil, boolean ativo, Long produtorId) {
    public static UsuarioSaida de(UsuarioSessao u) {
        return new UsuarioSaida(u.id(), u.nome(), u.email(), u.perfil(), true, u.produtorId());
    }
}
