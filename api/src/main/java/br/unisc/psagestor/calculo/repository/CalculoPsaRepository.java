package br.unisc.psagestor.calculo.repository;

import br.unisc.psagestor.calculo.model.CalculoPsa;
import br.unisc.psagestor.comum.repository.EntidadeRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

public interface CalculoPsaRepository extends EntidadeRepository<CalculoPsa, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }

    @Transactional
    default long criar(
            long diagnosticoId,
            long parametroId,
            Integer revisaoDiagnostico,
            String memoriaJson,
            BigDecimal areaContratada,
            BigDecimal valorHectare,
            BigDecimal percentual,
            BigDecimal pontuacao,
            BigDecimal valorAnual,
            long criadoPor) {
        var entity = new CalculoPsa();
        entity.setDiagnosticoId(diagnosticoId);
        entity.setParametroId(parametroId);
        entity.setRevisaoDiagnostico(revisaoDiagnostico);
        entity.setMemoriaJson(memoriaJson);
        entity.setAreaContratada(areaContratada);
        entity.setValorHectare(valorHectare);
        entity.setPercentual(percentual);
        entity.setPontuacao(pontuacao);
        entity.setValorAnual(valorAnual);
        entity.setCriadoEm(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setCriadoPor(criadoPor);
        return salvarEAtualizar(entity).getId();
    }

    @org.springframework.data.jpa.repository.Query(
            value =
                    "SELECT MAX(c.area_contratada) FROM calculo_psa c JOIN diagnostico d ON d.id=c.diagnostico_id WHERE d.propriedade_id=?1",
            nativeQuery = true)
    java.math.BigDecimal maiorAreaNaPropriedade(Long id);
}
