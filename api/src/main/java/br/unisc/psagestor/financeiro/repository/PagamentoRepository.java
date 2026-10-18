package br.unisc.psagestor.financeiro.repository;

import br.unisc.psagestor.comum.repository.EntidadeRepository;
import br.unisc.psagestor.financeiro.model.Pagamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public interface PagamentoRepository extends EntidadeRepository<Pagamento, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }

    @Transactional
    default long criar(
            long compromissoId,
            LocalDate dataPagamento,
            BigDecimal valor,
            String referencia,
            String chaveOperacao,
            String hashRequisicao,
            String observacao,
            long criadoPor) {
        var entity = new Pagamento();
        entity.setCompromissoId(compromissoId);
        entity.setDataPagamento(dataPagamento);
        entity.setValor(valor);
        entity.setReferencia(referencia);
        entity.setChaveOperacao(chaveOperacao);
        entity.setHashRequisicao(hashRequisicao);
        entity.setObservacao(observacao);
        entity.setCriadoEm(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setCriadoPor(criadoPor);
        return salvarEAtualizar(entity).getId();
    }

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT COALESCE(SUM(valor),0) FROM pagamento WHERE compromisso_id=?1",
            nativeQuery = true)
    BigDecimal somaPorCompromisso(Long id);

    Optional<Pagamento> findByChaveOperacao(String chaveOperacao);
}
