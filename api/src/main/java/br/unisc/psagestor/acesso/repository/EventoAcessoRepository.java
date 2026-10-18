package br.unisc.psagestor.acesso.repository;

import br.unisc.psagestor.acesso.model.EventoAcesso;
import br.unisc.psagestor.comum.repository.EntidadeRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

public interface EventoAcessoRepository extends EntidadeRepository<EventoAcesso, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }

    @Transactional
    default void registrar(String evento, String motivo, Long usuario, String requisicao) {
        var e = new EventoAcesso();
        e.setEvento(evento);
        e.setMotivo(motivo);
        e.setUsuarioId(usuario);
        e.setRequisicaoId(requisicao);
        e.setCriadoEm(LocalDateTime.now());
        saveAndFlush(e);
    }
}
