package br.unisc.psagestor.acesso.api;

import br.unisc.psagestor.acesso.model.UsuarioSessao;
import br.unisc.psagestor.acesso.service.AutenticacaoService;
import br.unisc.psagestor.acesso.service.EventosAcesso;
import br.unisc.psagestor.acesso.service.SenhaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AutenticacaoService service;
    private final SecurityContextRepository contexts;
    private final SessionAuthenticationStrategy sessions;
    private final SenhaService senhas;
    private final EventosAcesso eventos;

    public AuthController(
            AutenticacaoService service,
            SecurityContextRepository contexts,
            SessionAuthenticationStrategy sessions,
            SenhaService senhas,
            EventosAcesso eventos) {
        this.service = service;
        this.contexts = contexts;
        this.sessions = sessions;
        this.senhas = senhas;
        this.eventos = eventos;
    }

    public record LoginEntrada(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 200) String senha) {
        @Override
        public String toString() {
            return "LoginEntrada[credenciais omitidas]";
        }
    }

    public record SenhaEntrada(
            @NotBlank @Size(max = 200) String senhaAtual,
            @NotBlank @Size(min = 12, max = 72) String novaSenha) {
        @Override
        public String toString() {
            return "SenhaEntrada[credenciais omitidas]";
        }
    }

    public record CsrfSaida(String token, String headerName) {}

    @GetMapping("/csrf")
    public CsrfSaida csrf(CsrfToken token) {
        return new CsrfSaida(token.getToken(), token.getHeaderName());
    }

    @PostMapping("/login")
    public UsuarioSaida login(
            @Valid @RequestBody LoginEntrada entrada,
            HttpServletRequest request,
            HttpServletResponse response) {
        var principal = service.autenticar(entrada.email(), entrada.senha());
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.perfil().name()));
        var sessao =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
        eventos.encerrar(request.getSession(false), EventosAcesso.Motivo.REAUTENTICACAO);
        sessions.onAuthentication(sessao, request, response);
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(sessao);
        SecurityContextHolder.setContext(contexto);
        contexts.saveContext(contexto, request, response);
        eventos.iniciar(request.getSession(), principal.id());
        return UsuarioSaida.de(principal);
    }

    @GetMapping("/me")
    public UsuarioSaida me(@AuthenticationPrincipal UsuarioSessao usuario) {
        return UsuarioSaida.de(usuario);
    }

    @PutMapping("/senha")
    public ResponseEntity<Void> senha(
            @AuthenticationPrincipal UsuarioSessao usuario,
            @Valid @RequestBody SenhaEntrada entrada,
            HttpServletRequest request,
            HttpServletResponse response) {
        senhas.alterar(usuario.id(), entrada.senhaAtual(), entrada.novaSenha());
        eventos.encerrar(request.getSession(false), EventosAcesso.Motivo.SENHA_ALTERADA);
        new SecurityContextLogoutHandler()
                .logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        return ResponseEntity.noContent().build();
    }
}
