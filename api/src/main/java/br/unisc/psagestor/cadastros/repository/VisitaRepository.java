package br.unisc.psagestor.cadastros.repository;

import br.unisc.psagestor.cadastros.model.Visita;
import br.unisc.psagestor.comum.repository.EntidadeRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

public interface VisitaRepository extends EntidadeRepository<Visita, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }

    @Transactional
    default long criar(
            long propriedadeId,
            long tecnicoId,
            LocalDate dataVisita,
            String observacoes,
            long criadoPor) {
        var entity = new Visita();
        entity.setPropriedadeId(propriedadeId);
        entity.setTecnicoId(tecnicoId);
        entity.setDataVisita(dataVisita);
        entity.setObservacoes(observacoes);
        entity.setCriadoEm(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setCriadoPor(criadoPor);
        return salvarEAtualizar(entity).getId();
    }

    @Transactional
    default void atualizarVisita(
            LocalDate dataVisita, String observacoes, Long atualizadoPor, Long id) {
        var entity = obter(id);
        entity.setDataVisita(dataVisita);
        entity.setObservacoes(observacoes);
        entity.setAtualizadoEm(
                LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setAtualizadoPor(atualizadoPor);
        salvarEAtualizar(entity);
    }
}
