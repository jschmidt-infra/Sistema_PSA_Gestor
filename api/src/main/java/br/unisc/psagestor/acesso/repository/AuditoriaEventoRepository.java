package br.unisc.psagestor.acesso.repository;

import br.unisc.psagestor.acesso.model.AuditoriaEvento;
import br.unisc.psagestor.comum.repository.EntidadeRepository;
import java.util.Map;

public interface AuditoriaEventoRepository extends EntidadeRepository<AuditoriaEvento, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }
}
