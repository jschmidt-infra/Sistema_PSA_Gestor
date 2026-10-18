package br.unisc.psagestor.configuracao.repository;

import br.unisc.psagestor.comum.repository.EntidadeRepository;
import br.unisc.psagestor.configuracao.model.Projeto;
import java.util.Map;

public interface ProjetoRepository extends EntidadeRepository<Projeto, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }
}
