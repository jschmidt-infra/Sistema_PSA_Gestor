package br.unisc.psagestor.acesso.api;

import br.unisc.psagestor.acesso.service.EventosAcesso;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Observa somente o resultado HTTP; nunca lê o corpo ou os cabeçalhos da requisição. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuditoriaLoginFilter extends OncePerRequestFilter {
    private final EventosAcesso eventos;

    public AuditoriaLoginFilter(EventosAcesso eventos) {
        this.eventos = eventos;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(req, res);
        if (!"POST".equals(req.getMethod())
                || !"/api/v1/auth/login"
                        .equals(
                                req.getServletPath().isEmpty()
                                        ? req.getRequestURI()
                                        : req.getServletPath())) return;
        int status = res.getStatus();
        if (status < 400) return;
        var motivo =
                switch (status) {
                    case 401 -> EventosAcesso.Motivo.CREDENCIAIS_RECUSADAS;
                    case 429 -> EventosAcesso.Motivo.BLOQUEIO_TEMPORARIO;
                    case 403 -> EventosAcesso.Motivo.CSRF_OU_ACESSO_NEGADO;
                    default ->
                            status >= 500
                                    ? EventosAcesso.Motivo.SERVICO_INDISPONIVEL
                                    : EventosAcesso.Motivo.REQUISICAO_INVALIDA;
                };
        eventos.registrar(EventosAcesso.Evento.LOGIN_RECUSADO, motivo, null);
    }
}
