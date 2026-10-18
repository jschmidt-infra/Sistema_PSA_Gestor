package br.unisc.psagestor.financeiro.repository;

import br.unisc.psagestor.comum.repository.EntidadeRepository;
import br.unisc.psagestor.financeiro.model.CompromissoAnual;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

public interface CompromissoAnualRepository extends EntidadeRepository<CompromissoAnual, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }

    @Transactional
    default long criar(long contratoId, Integer exercicio, BigDecimal valor, long criadoPor) {
        var entity = new CompromissoAnual();
        entity.setContratoId(contratoId);
        entity.setExercicio(exercicio);
        entity.setValor(valor);
        entity.setCriadoEm(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setCriadoPor(criadoPor);
        return salvarEAtualizar(entity).getId();
    }
}
