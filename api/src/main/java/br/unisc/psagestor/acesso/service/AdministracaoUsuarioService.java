package br.unisc.psagestor.acesso.service;

import br.unisc.psagestor.acesso.api.UsuariosEntradas.Criar;
import br.unisc.psagestor.acesso.api.UsuariosEntradas.Entrada;
import br.unisc.psagestor.acesso.api.UsuariosEntradas.Senha;
import br.unisc.psagestor.acesso.model.Perfil;
import br.unisc.psagestor.acesso.repository.AdministracaoUsuarioRepository;
import br.unisc.psagestor.comum.api.FalhaApi;
import br.unisc.psagestor.comum.api.RegraNegocioException;
import br.unisc.psagestor.comum.api.SessaoAtual;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdministracaoUsuarioService {
    private final AdministracaoUsuarioRepository repository;
    private final PasswordEncoder encoder;

    public AdministracaoUsuarioService(
            AdministracaoUsuarioRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    public Object listar(int page, int size) {
        return repository.listar(page, size);
    }

    private Entrada validar(Entrada e) {
        if ((e.perfil() == Perfil.PRODUTOR) != (e.produtorId() != null))
            throw new RegraNegocioException(
                    "VINCULO_INVALIDO", "Somente produtores devem possuir produtorId.");
        if (e.produtorId() != null) repository.exigirProdutor(e.produtorId());
        return new Entrada(
                e.nome(),
                e.email().strip().toLowerCase(Locale.ROOT),
                e.perfil(),
                e.produtorId(),
                e.ativo());
    }

    private String hash(String senha) {
        SenhaService.validarNovaSenha(senha);
        return encoder.encode(senha);
    }

    @Transactional
    public Map<String, Object> criar(Criar c) {
        var e = validar(new Entrada(c.nome(), c.email(), c.perfil(), c.produtorId(), c.ativo()));
        long id = repository.inserir(e, hash(c.senha()), SessaoAtual.id());
        repository.auditar("CRIAR", id);
        return repository.consultar(id);
    }

    @Transactional
    public Object editar(long id, Entrada entrada) {
        var e = validar(entrada);
        repository.bloquearAdministradores();
        var anterior = repository.bloquear(id);
        if ("ADMINISTRADOR".equals(anterior.get("perfil"))
                && (!e.ativo() || e.perfil() != Perfil.ADMINISTRADOR)
                && repository.outrosAdministradoresAtivos(id) == 0)
            throw FalhaApi.conflito("É necessário manter pelo menos um administrador ativo.");
        if (!Objects.equals(anterior.get("produtor_id"), e.produtorId()))
            throw FalhaApi.conflito(
                    "O vínculo com produtor não pode ser transferido; crie outra conta.");
        repository.editar(id, e, SessaoAtual.id());
        repository.auditar("EDITAR", id);
        return repository.consultar(id);
    }

    @Transactional
    public void senha(long id, Senha entrada) {
        repository.bloquear(id);
        repository.senha(id, hash(entrada.novaSenha()), SessaoAtual.id());
        repository.auditar("REDEFINIR_SENHA", id);
    }
}
