package br.unisc.psagestor.acesso.repository;

import br.unisc.psagestor.acesso.model.Usuario;
import br.unisc.psagestor.comum.repository.EntidadeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public interface UsuarioRepository extends EntidadeRepository<Usuario, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }

    @Transactional
    default long criar(
            String nome,
            String email,
            String senhaHash,
            String perfil,
            Boolean ativo,
            Long produtorId,
            Long criadoPor) {
        var entity = new Usuario();
        entity.setNome(nome);
        entity.setEmail(email);
        entity.setSenhaHash(senhaHash);
        entity.setPerfil(perfil);
        entity.setAtivo(ativo);
        entity.setProdutorId(produtorId);
        entity.setCriadoEm(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setCriadoPor(criadoPor);
        return salvarEAtualizar(entity).getId();
    }

    @Transactional
    default void redefinirSenha(String senhaHash, Long atualizadoPor, Long id) {
        var entity = obter(id);
        entity.setSenhaHash(senhaHash);
        entity.setVersaoCredenciais(entity.getVersaoCredenciais() + 1);
        entity.setAtualizadoEm(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setAtualizadoPor(atualizadoPor);
        salvarEAtualizar(entity);
    }

    @Transactional
    default void atualizarCadastro(
            String nome, String email, String perfil, Boolean ativo, Long atualizadoPor, Long id) {
        var entity = obter(id);
        entity.setNome(nome);
        entity.setEmail(email);
        entity.setPerfil(perfil);
        entity.setAtivo(ativo);
        entity.setVersaoCredenciais(entity.getVersaoCredenciais() + 1);
        entity.setAtualizadoEm(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setAtualizadoPor(atualizadoPor);
        salvarEAtualizar(entity);
    }

    Optional<Usuario> findByEmail(String email);

    @org.springframework.data.jpa.repository.Query(
            value =
                    "SELECT COUNT(*) FROM usuario WHERE perfil='ADMINISTRADOR' AND ativo=TRUE AND id<>?1",
            nativeQuery = true)
    Long outrosAdministradoresAtivos(Long id);

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT id FROM usuario WHERE perfil='ADMINISTRADOR' ORDER BY id FOR UPDATE",
            nativeQuery = true)
    List<Long> bloquearAdministradores();
}
