package br.unisc.psagestor.acesso.api;

import br.unisc.psagestor.acesso.service.EventosAcesso;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventosAcessoConfig {
    @Bean
    ServletListenerRegistrationBean<HttpSessionListener> auditoriaEncerramentoSessao(
            EventosAcesso eventos) {
        return new ServletListenerRegistrationBean<>(
                new HttpSessionListener() {
                    @Override
                    public void sessionDestroyed(HttpSessionEvent event) {
                        eventos.encerrar(
                                event.getSession(),
                                EventosAcesso.Motivo.EXPIRACAO_OU_ENCERRAMENTO_CONTAINER);
                    }
                });
    }
}
