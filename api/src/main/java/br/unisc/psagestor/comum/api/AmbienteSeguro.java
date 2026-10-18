package br.unisc.psagestor.comum.api;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class AmbienteSeguro {
    @Bean
    ApplicationRunner validarAmbiente(Environment env) {
        return args -> {
            if (env.matchesProfiles("prod")
                    && (env.matchesProfiles("test", "demonstracao")
                            || !env.getProperty("spring.datasource.url", "")
                                    .startsWith("jdbc:mariadb:")))
                throw new IllegalStateException(
                        "Produção não pode usar perfis ou banco de testes.");
        };
    }
}
