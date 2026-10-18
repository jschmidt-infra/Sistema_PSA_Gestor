package br.unisc.psagestor.financeiro.repository;

import br.unisc.psagestor.comum.repository.EntidadeRepository;
import br.unisc.psagestor.financeiro.model.Aporte;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

public interface AporteRepository extends EntidadeRepository<Aporte, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }

    @Transactional
    default long criar(
            long projetoId,
            String tipo,
            LocalDate dataAporte,
            BigDecimal valor,
            String descricao,
            long criadoPor) {
        var entity = new Aporte();
        entity.setProjetoId(projetoId);
        entity.setTipo(tipo);
        entity.setDataAporte(dataAporte);
        entity.setValor(valor);
        entity.setDescricao(descricao);
        entity.setCriadoEm(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setCriadoPor(criadoPor);
        return salvarEAtualizar(entity).getId();
    }

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT COUNT(*) FROM aporte WHERE projeto_id=?1 AND tipo='SALDO_INICIAL'",
            nativeQuery = true)
    Long quantidadeSaldosIniciais(Long id);
}
