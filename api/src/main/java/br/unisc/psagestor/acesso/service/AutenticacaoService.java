package br.unisc.psagestor.acesso.service;

import br.unisc.psagestor.acesso.model.UsuarioSessao;
import br.unisc.psagestor.acesso.repository.UsuarioRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoService {
    private final LimiteLogin limite;
    private final UsuarioRepository usuarios;

    public AutenticacaoService(LimiteLogin limite, UsuarioRepository usuarios) {
        this.limite = limite;
        this.usuarios = usuarios;
    }

    public UsuarioSessao autenticar(String email, String senha) {
        var autenticado = limite.autenticar(email, senha);
        var usuario =
                usuarios.findByEmail(autenticado.getName())
                        .orElseThrow(() -> new BadCredentialsException("Credenciais não aceitas."));
        if (!usuario.isAtivo()) throw new BadCredentialsException("Credenciais não aceitas.");
        return UsuarioSessao.de(usuario);
    }
}
