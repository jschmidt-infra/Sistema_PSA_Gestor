package br.unisc.psagestor.arquivos.repository;

import br.unisc.psagestor.arquivos.model.Croqui;
import br.unisc.psagestor.comum.repository.EntidadeRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

public interface CroquiRepository extends EntidadeRepository<Croqui, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }

    @Transactional
    default long criar(
            long propriedadeId,
            String nomeOriginal,
            String chave,
            String tipoMime,
            long tamanho,
            BigDecimal areaPsa,
            String observacoes,
            long criadoPor) {
        var entity = new Croqui();
        entity.setPropriedadeId(propriedadeId);
        entity.setNomeOriginal(nomeOriginal);
        entity.setChave(chave);
        entity.setTipoMime(tipoMime);
        entity.setTamanho(tamanho);
        entity.setAreaPsa(areaPsa);
        entity.setObservacoes(observacoes);
        entity.setCriadoEm(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setCriadoPor(criadoPor);
        return salvarEAtualizar(entity).getId();
    }

    @Transactional
    default void substituirArquivo(
            String nomeOriginal,
            String chave,
            String tipoMime,
            long tamanho,
            BigDecimal areaPsa,
            String observacoes,
            Long id) {
        var entity = obter(id);
        entity.setNomeOriginal(nomeOriginal);
        entity.setChave(chave);
        entity.setTipoMime(tipoMime);
        entity.setTamanho(tamanho);
        entity.setAreaPsa(areaPsa);
        entity.setObservacoes(observacoes);
        salvarEAtualizar(entity);
    }

    long countByChave(String chave);
}
