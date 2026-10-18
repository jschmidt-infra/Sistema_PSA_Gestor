package br.unisc.psagestor.diagnostico.repository;

import br.unisc.psagestor.comum.repository.EntidadeRepository;
import br.unisc.psagestor.diagnostico.model.Diagnostico;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

public interface DiagnosticoRepository extends EntidadeRepository<Diagnostico, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }

    @Transactional
    default long criar(
            long propriedadeId,
            long projetoId,
            long parametroId,
            long tecnicoId,
            LocalDate dataVisita,
            long criadoPor) {
        var entity = new Diagnostico();
        entity.setPropriedadeId(propriedadeId);
        entity.setProjetoId(projetoId);
        entity.setParametroId(parametroId);
        entity.setTecnicoId(tecnicoId);
        entity.setDataVisita(dataVisita);
        entity.setStatus("RASCUNHO");
        entity.setRevisao(1);
        entity.setRespostasJson("[]");
        entity.setCriadoEm(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setCriadoPor(criadoPor);
        return salvarEAtualizar(entity).getId();
    }

    @Transactional
    default void reabrir(String motivoCorrecao, Long id) {
        var entity = obter(id);
        entity.setStatus("EM_CORRECAO");
        entity.setRevisao(entity.getRevisao() + 1);
        entity.setPontuacao(null);
        entity.setMemoriaJson(null);
        entity.setMotivoCorrecao(motivoCorrecao);
        entity.setConcluidoEm(null);
        salvarEAtualizar(entity);
    }

    @Transactional
    default void concluir(BigDecimal pontuacao, String memoriaJson, Long id) {
        var entity = obter(id);
        entity.setStatus("CONCLUIDO");
        entity.setPontuacao(pontuacao);
        entity.setMemoriaJson(memoriaJson);
        entity.setConcluidoEm(
                LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        salvarEAtualizar(entity);
    }

    @Transactional
    default void registrarRespostas(String respostasJson, Long id) {
        var entity = obter(id);
        entity.setRespostasJson(respostasJson);
        entity.setPontuacao(null);
        entity.setMemoriaJson(null);
        salvarEAtualizar(entity);
    }
}
