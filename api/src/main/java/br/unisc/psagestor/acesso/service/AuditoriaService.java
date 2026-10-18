package br.unisc.psagestor.acesso.service;

import br.unisc.psagestor.acesso.model.AuditoriaEvento;
import br.unisc.psagestor.acesso.repository.AuditoriaEventoRepository;
import br.unisc.psagestor.comum.api.SessaoAtual;
import java.time.LocalDateTime;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {
    private final AuditoriaEventoRepository repository;

    public AuditoriaService(AuditoriaEventoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void registrar(String operacao, String recurso, long id) {
        var e = new AuditoriaEvento();
        e.setUsuarioId(SessaoAtual.id());
        e.setOperacao(operacao);
        e.setRecurso(recurso);
        e.setRecursoId(id);
        e.setCriadoEm(LocalDateTime.now());
        e.setRequisicaoId(MDC.get("requisicaoId"));
        repository.saveAndFlush(e);
    }
}
