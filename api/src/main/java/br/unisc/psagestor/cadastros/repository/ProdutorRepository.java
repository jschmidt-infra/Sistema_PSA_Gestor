package br.unisc.psagestor.cadastros.repository;

import br.unisc.psagestor.cadastros.model.Produtor;
import br.unisc.psagestor.comum.repository.EntidadeRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

public interface ProdutorRepository extends EntidadeRepository<Produtor, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }

    @Transactional
    default long criar(
            String nome,
            String documento,
            String endereco,
            String telefone,
            String email,
            long criadoPor) {
        var entity = new Produtor();
        entity.setNome(nome);
        entity.setDocumento(documento);
        entity.setEndereco(endereco);
        entity.setTelefone(telefone);
        entity.setEmail(email);
        entity.setCriadoEm(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setCriadoPor(criadoPor);
        return salvarEAtualizar(entity).getId();
    }

    @Transactional
    default void atualizarCadastro(
            String nome,
            String documento,
            String endereco,
            String telefone,
            String email,
            Long atualizadoPor,
            Long id) {
        var entity = obter(id);
        entity.setNome(nome);
        entity.setDocumento(documento);
        entity.setEndereco(endereco);
        entity.setTelefone(telefone);
        entity.setEmail(email);
        entity.setAtualizadoEm(
                LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setAtualizadoPor(atualizadoPor);
        salvarEAtualizar(entity);
    }
}
