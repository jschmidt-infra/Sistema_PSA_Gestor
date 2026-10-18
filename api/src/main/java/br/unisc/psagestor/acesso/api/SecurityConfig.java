package br.unisc.psagestor.acesso.api;

import br.unisc.psagestor.acesso.repository.UsuarioRepository;
import br.unisc.psagestor.acesso.service.EventosAcesso;
import br.unisc.psagestor.acesso.service.UsuarioDetailsService;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(
            UsuarioDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrf) {
        return new CompositeSessionAuthenticationStrategy(
                List.of(
                        new ChangeSessionIdAuthenticationStrategy(),
                        new CsrfAuthenticationStrategy(csrf)));
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository contexts,
            CsrfTokenRepository csrf,
            UsuarioRepository usuarios,
            JsonMapper mapper,
            EventosAcesso eventos)
            throws Exception {
        var respostas = new RespostaSeguranca(mapper);
        return http.formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .csrf(c -> c.csrfTokenRepository(csrf))
                .securityContext(c -> c.securityContextRepository(contexts))
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                HttpMethod.GET,
                                                "/api/v1/auth/csrf",
                                                "/actuator/health/liveness")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me")
                                        .authenticated()
                                        .requestMatchers(HttpMethod.PUT, "/api/v1/auth/senha")
                                        .authenticated()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/actuator/health",
                                                "/actuator/health/**",
                                                "/actuator/metrics",
                                                "/actuator/metrics/**")
                                        .hasRole("ADMINISTRADOR")
                                        .requestMatchers("/api/v1/**")
                                        .authenticated()
                                        .requestMatchers(
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .hasRole("ADMINISTRADOR")
                                        .anyRequest()
                                        .denyAll())
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                                (req, res, ex) ->
                                                        respostas.escrever(
                                                                req,
                                                                res,
                                                                401,
                                                                "NAO_AUTENTICADO",
                                                                "Entre para continuar."))
                                        .accessDeniedHandler(
                                                (req, res, ex) ->
                                                        respostas.escrever(
                                                                req,
                                                                res,
                                                                403,
                                                                "ACESSO_NEGADO",
                                                                "Permissão ou token CSRF inválido.")))
                .logout(
                        l ->
                                l.logoutUrl("/api/v1/auth/logout")
                                        .invalidateHttpSession(true)
                                        .deleteCookies("PSA_SESSION")
                                        .addLogoutHandler(
                                                (req, res, auth) ->
                                                        eventos.encerrar(
                                                                req.getSession(false),
                                                                EventosAcesso.Motivo.LOGOUT))
                                        .logoutSuccessHandler(
                                                (req, res, auth) -> res.setStatus(204)))
                .addFilterBefore(
                        new ValidarSessaoFilter(usuarios, respostas, eventos),
                        AuthorizationFilter.class)
                .build();
    }
}
