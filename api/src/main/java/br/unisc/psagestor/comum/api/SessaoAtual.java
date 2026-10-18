package br.unisc.psagestor.comum.api;

import br.unisc.psagestor.acesso.model.Perfil;
import br.unisc.psagestor.acesso.model.UsuarioSessao;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SessaoAtual {
    private SessaoAtual() {}

    public static UsuarioSessao usuario() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioSessao u))
            throw new AccessDeniedException("Sessão necessária.");
        return u;
    }

    public static long id() {
        return usuario().id();
    }

    public static void verificarProdutor(long id) {
        var u = usuario();
        if (u.perfil() == Perfil.PRODUTOR && !Long.valueOf(id).equals(u.produtorId()))
            throw FalhaApi.inexistente();
    }
}
