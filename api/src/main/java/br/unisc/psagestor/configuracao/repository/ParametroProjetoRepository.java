package br.unisc.psagestor.configuracao.repository;

import br.unisc.psagestor.comum.repository.EntidadeRepository;
import br.unisc.psagestor.configuracao.model.ParametroProjeto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public interface ParametroProjetoRepository extends EntidadeRepository<ParametroProjeto, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }

    @Transactional
    default long criar(
            long projetoId,
            Integer versao,
            BigDecimal valorHectare,
            BigDecimal percentual,
            String configuracaoJson,
            long criadoPor) {
        var entity = new ParametroProjeto();
        entity.setProjetoId(projetoId);
        entity.setVersao(versao);
        entity.setValorHectare(valorHectare);
        entity.setPercentual(percentual);
        entity.setConfiguracaoJson(configuracaoJson);
        entity.setCriadoEm(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setCriadoPor(criadoPor);
        return salvarEAtualizar(entity).getId();
    }

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT COALESCE(MAX(versao),0)+1 FROM parametro_projeto WHERE projeto_id=?1",
            nativeQuery = true)
    Integer proximaVersao(Long id);

    Optional<ParametroProjeto> findFirstByProjetoIdOrderByVersaoDesc(Long projetoId);
}
