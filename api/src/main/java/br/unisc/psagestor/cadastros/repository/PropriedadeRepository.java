package br.unisc.psagestor.cadastros.repository;

import br.unisc.psagestor.cadastros.model.Propriedade;
import br.unisc.psagestor.comum.repository.EntidadeRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

public interface PropriedadeRepository extends EntidadeRepository<Propriedade, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }

    @Transactional
    default long criar(
            long produtorId,
            long localidadeId,
            Long subBaciaId,
            String nome,
            BigDecimal areaTotal,
            String carSituacao,
            String carNumero,
            long criadoPor) {
        var entity = new Propriedade();
        entity.setProdutorId(produtorId);
        entity.setLocalidadeId(localidadeId);
        entity.setSubBaciaId(subBaciaId);
        entity.setNome(nome);
        entity.setAreaTotal(areaTotal);
        entity.setCarSituacao(carSituacao);
        entity.setCarNumero(carNumero);
        entity.setCriadoEm(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setCriadoPor(criadoPor);
        return salvarEAtualizar(entity).getId();
    }

    @Transactional
    default void alterarFontesSituacao(String fontesSituacao, Long atualizadoPor, Long id) {
        var entity = obter(id);
        entity.setFontesSituacao(fontesSituacao);
        entity.setAtualizadoEm(
                LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setAtualizadoPor(atualizadoPor);
        salvarEAtualizar(entity);
    }

    @Transactional
    default void alterarFontesSituacao(String fontesSituacao, Long id) {
        var entity = obter(id);
        entity.setFontesSituacao(fontesSituacao);
        salvarEAtualizar(entity);
    }

    @Transactional
    default void atualizarCadastro(
            long localidadeId,
            Long subBaciaId,
            String nome,
            BigDecimal areaTotal,
            String carSituacao,
            String carNumero,
            Long atualizadoPor,
            Long id) {
        var entity = obter(id);
        entity.setLocalidadeId(localidadeId);
        entity.setSubBaciaId(subBaciaId);
        entity.setNome(nome);
        entity.setAreaTotal(areaTotal);
        entity.setCarSituacao(carSituacao);
        entity.setCarNumero(carNumero);
        entity.setAtualizadoEm(
                LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setAtualizadoPor(atualizadoPor);
        salvarEAtualizar(entity);
    }
}
