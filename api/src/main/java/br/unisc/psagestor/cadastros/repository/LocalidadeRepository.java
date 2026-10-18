package br.unisc.psagestor.cadastros.repository;

import br.unisc.psagestor.cadastros.model.Localidade;
import br.unisc.psagestor.comum.repository.EntidadeRepository;
import java.util.Map;

public interface LocalidadeRepository extends EntidadeRepository<Localidade, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }
}
