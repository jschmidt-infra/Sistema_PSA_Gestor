package br.unisc.psagestor.acesso.service;

import br.unisc.psagestor.acesso.repository.UsuarioRepository;
import br.unisc.psagestor.comum.api.RegraNegocioException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SenhaService {
    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;

    public SenhaService(UsuarioRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    public static void validarNovaSenha(String senha) {
        if (senha == null
                || senha.length() < 12
                || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RegraNegocioException(
                    "SENHA_INVALIDA",
                    "A senha deve ter ao menos 12 caracteres e no máximo 72 bytes UTF-8.");
        }
    }

    @Transactional
    public void alterar(Long id, String atual, String nova) {
        var usuario =
                repository
                        .findById(id)
                        .orElseThrow(() -> new BadCredentialsException("Sessão inválida."));
        if (!encoder.matches(atual, usuario.getSenhaHash())) {
            throw new BadCredentialsException("Credenciais não aceitas.");
        }
        validarNovaSenha(nova);
        usuario.alterarSenha(encoder.encode(nova), id);
    }
}
