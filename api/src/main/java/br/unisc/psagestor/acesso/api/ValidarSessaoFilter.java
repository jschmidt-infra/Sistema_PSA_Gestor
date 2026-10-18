package br.unisc.psagestor.acesso.api;

import br.unisc.psagestor.acesso.model.UsuarioSessao;
import br.unisc.psagestor.acesso.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Revalida conta e perfil no banco; permissões nunca são inferidas de cache. */
public class ValidarSessaoFilter extends OncePerRequestFilter {
    private final UsuarioRepository usuarios;
    private final RespostaSeguranca respostas;
    private final br.unisc.psagestor.acesso.service.EventosAcesso eventos;

    public ValidarSessaoFilter(
            UsuarioRepository usuarios,
            RespostaSeguranca respostas,
            br.unisc.psagestor.acesso.service.EventosAcesso eventos) {
        this.usuarios = usuarios;
        this.respostas = respostas;
        this.eventos = eventos;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioSessao sessao) {
            boolean valida;
            try {
                valida =
                        usuarios.findById(sessao.id())
                                .filter(
                                        u ->
                                                u.isAtivo()
                                                        && u.getPerfil() == sessao.perfil()
                                                        && u.getVersaoCredenciais()
                                                                == sessao.versaoCredenciais()
                                                        && Objects.equals(
                                                                u.getProdutorId(),
                                                                sessao.produtorId()))
                                .isPresent();
            } catch (DataAccessException ex) {
                respostas.escrever(
                        request,
                        response,
                        503,
                        "SERVICO_INDISPONIVEL",
                        "Não foi possível validar a sessão.");
                return;
            }
            if (!valida) {
                var httpSession = request.getSession(false);
                eventos.encerrar(
                        httpSession,
                        br.unisc.psagestor.acesso.service.EventosAcesso.Motivo.SESSAO_REVOGADA);
                if (httpSession != null) httpSession.invalidate();
                SecurityContextHolder.clearContext();
                respostas.escrever(
                        request,
                        response,
                        401,
                        "SESSAO_INVALIDA",
                        "Entre novamente para continuar.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
