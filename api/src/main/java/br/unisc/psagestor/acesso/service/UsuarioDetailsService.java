package br.unisc.psagestor.acesso.service;

import br.unisc.psagestor.acesso.repository.UsuarioRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository repository;

    public UsuarioDetailsService(UsuarioRepository repository) {
        this.repository = repository;
    }

    // Conta inexistente é recusa de credenciais; deve preservar o contador da tentativa.
    @Override
    @Transactional(readOnly = true, noRollbackFor = UsernameNotFoundException.class)
    public UserDetails loadUserByUsername(String email) {
        var usuario =
                repository
                        .findByEmail(email.strip().toLowerCase(Locale.ROOT))
                        .orElseThrow(
                                () -> new UsernameNotFoundException("Credenciais não aceitas."));
        return User.withUsername(usuario.getEmail())
                .password(usuario.getSenhaHash())
                .roles(usuario.getPerfil().name())
                .disabled(!usuario.isAtivo())
                .build();
    }
}
