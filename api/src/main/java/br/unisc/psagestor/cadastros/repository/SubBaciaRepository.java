package br.unisc.psagestor.cadastros.repository;

import br.unisc.psagestor.cadastros.model.SubBacia;
import br.unisc.psagestor.comum.repository.EntidadeRepository;
import java.util.Map;

public interface SubBaciaRepository extends EntidadeRepository<SubBacia, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }
}
