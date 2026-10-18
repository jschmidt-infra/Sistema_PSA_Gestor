package br.unisc.psagestor.cadastros.repository;

import br.unisc.psagestor.cadastros.model.FonteHidrica;
import br.unisc.psagestor.comum.repository.EntidadeRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

public interface FonteHidricaRepository extends EntidadeRepository<FonteHidrica, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }

    @Transactional
    default long criar(
            long propriedadeId,
            String tipo,
            String protecao,
            Boolean ativo,
            String observacoes,
            long criadoPor) {
        var entity = new FonteHidrica();
        entity.setPropriedadeId(propriedadeId);
        entity.setTipo(tipo);
        entity.setProtecao(protecao);
        entity.setAtivo(ativo);
        entity.setObservacoes(observacoes);
        entity.setCriadoEm(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setCriadoPor(criadoPor);
        return salvarEAtualizar(entity).getId();
    }

    @Transactional
    default void atualizarFonte(
            String tipo,
            String protecao,
            Boolean ativo,
            String observacoes,
            Long atualizadoPor,
            Long id) {
        var entity = obter(id);
        entity.setTipo(tipo);
        entity.setProtecao(protecao);
        entity.setAtivo(ativo);
        entity.setObservacoes(observacoes);
        entity.setAtualizadoEm(
                LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setAtualizadoPor(atualizadoPor);
        salvarEAtualizar(entity);
    }

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT COUNT(*) FROM fonte_hidrica WHERE propriedade_id=?1 AND ativo=TRUE",
            nativeQuery = true)
    Long quantidadeAtivas(Long id);
}
