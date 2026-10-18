package br.unisc.psagestor.comum.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequisicaoIdFilter extends OncePerRequestFilter {
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(RequisicaoIdFilter.class);
    public static final String ATRIBUTO = "requisicaoId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = UUID.randomUUID().toString();
        request.setAttribute(ATRIBUTO, id);
        response.setHeader("X-Request-ID", id);
        long inicio = System.nanoTime();
        try (var ignored = MDC.putCloseable(ATRIBUTO, id)) {
            boolean falhou = false;
            try {
                chain.doFilter(request, response);
            } catch (ServletException | IOException | RuntimeException e) {
                falhou = true;
                throw e;
            } finally {
                Object rota =
                        request.getAttribute(
                                org.springframework.web.servlet.HandlerMapping
                                        .BEST_MATCHING_PATTERN_ATTRIBUTE);
                String metodo =
                        java.util.Set.of(
                                                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS",
                                                "HEAD", "TRACE")
                                        .contains(request.getMethod())
                                ? request.getMethod()
                                : "OUTRO";
                // Somente o padrão da rota: não registrar URL, query string, corpo ou credenciais.
                LOG.info(
                        "requisicao_http metodo={} rota={} status={} duracaoMs={}",
                        metodo,
                        rota == null ? "NAO_MAPEADA" : rota,
                        falhou ? 500 : response.getStatus(),
                        java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                                System.nanoTime() - inicio));
            }
        }
    }
}
