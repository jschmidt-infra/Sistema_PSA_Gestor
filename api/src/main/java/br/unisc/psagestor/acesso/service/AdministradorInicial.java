package br.unisc.psagestor.acesso.service;

import br.unisc.psagestor.acesso.model.Perfil;
import br.unisc.psagestor.acesso.model.Usuario;
import br.unisc.psagestor.acesso.repository.UsuarioRepository;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdministradorInicial implements ApplicationRunner {
    private final BootstrapProperties config;
    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;

    public AdministradorInicial(
            BootstrapProperties config, UsuarioRepository repository, PasswordEncoder encoder) {
        this.config = config;
        this.repository = repository;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!config.enabled()) return;
        if (config.nome() == null
                || config.nome().isBlank()
                || config.email() == null
                || !config.email().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalStateException(
                    "Configure nome e email válidos para o administrador inicial.");
        }
        var email = config.email().strip().toLowerCase(Locale.ROOT);
        var existente = repository.findByEmail(email);
        if (existente.isPresent()) {
            if (existente.get().getPerfil() != Perfil.ADMINISTRADOR) {
                throw new IllegalStateException("O email inicial já pertence a outro perfil.");
            }
            return; // Nunca redefinir uma senha existente durante a inicialização.
        }
        SenhaService.validarNovaSenha(config.senha());
        repository.save(
                new Usuario(
                        config.nome(),
                        email,
                        encoder.encode(config.senha()),
                        Perfil.ADMINISTRADOR,
                        null,
                        null));
    }
}
