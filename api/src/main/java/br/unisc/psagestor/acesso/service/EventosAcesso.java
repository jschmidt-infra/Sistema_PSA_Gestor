package br.unisc.psagestor.acesso.service;

import br.unisc.psagestor.acesso.repository.EventoAcessoRepository;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Não recebe credenciais, e-mail, IP, cabeçalhos nem identificador HTTP de sessão. */
@Service
public class EventosAcesso {
    public enum Evento {
        LOGIN_SUCESSO,
        LOGIN_RECUSADO,
        SESSAO_ENCERRADA
    }

    public enum Motivo {
        AUTENTICADO,
        CREDENCIAIS_RECUSADAS,
        BLOQUEIO_TEMPORARIO,
        REQUISICAO_INVALIDA,
        CSRF_OU_ACESSO_NEGADO,
        SERVICO_INDISPONIVEL,
        LOGOUT,
        SENHA_ALTERADA,
        SESSAO_REVOGADA,
        REAUTENTICACAO,
        EXPIRACAO_OU_ENCERRAMENTO_CONTAINER
    }

    private static final Logger LOG = LoggerFactory.getLogger(EventosAcesso.class);
    private static final String ATRIBUTO = EventosAcesso.class.getName() + ".estado";

    private static final class Estado implements java.io.Serializable {
        final long usuarioId;
        boolean encerrada;

        Estado(long usuarioId) {
            this.usuarioId = usuarioId;
        }
    }

    private final EventoAcessoRepository repository;
    private final TransactionTemplate tx;

    public EventosAcesso(EventoAcessoRepository repository, PlatformTransactionManager manager) {
        this.repository = repository;
        tx = new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void registrar(Evento evento, Motivo motivo, Long usuarioId) {
        String id = MDC.get("requisicaoId");
        if (id != null && !id.matches("[a-fA-F0-9-]{36}")) id = null;
        final String requisicao = id;
        try {
            tx.executeWithoutResult(
                    s -> repository.registrar(evento.name(), motivo.name(), usuarioId, requisicao));
        } catch (RuntimeException e) {
            // Logout/revogação devem encerrar a sessão mesmo sem banco; não imprimir a exceção.
            LOG.warn(
                    "evento_acesso persistencia=indisponivel evento={} motivo={} usuarioId={} requisicaoId={}",
                    evento,
                    motivo,
                    usuarioId,
                    requisicao);
            return;
        }
        LOG.info(
                "evento_acesso evento={} motivo={} usuarioId={} requisicaoId={}",
                evento,
                motivo,
                usuarioId,
                requisicao);
    }

    public void iniciar(HttpSession session, long usuarioId) {
        session.setAttribute(ATRIBUTO, new Estado(usuarioId));
        registrar(Evento.LOGIN_SUCESSO, Motivo.AUTENTICADO, usuarioId);
    }

    public void encerrar(HttpSession session, Motivo motivo) {
        if (session == null) return;
        Object valor;
        try {
            valor = session.getAttribute(ATRIBUTO);
        } catch (IllegalStateException e) {
            return;
        }
        if (!(valor instanceof Estado estado)) return;
        synchronized (estado) {
            if (estado.encerrada) return;
            estado.encerrada = true;
        }
        registrar(Evento.SESSAO_ENCERRADA, motivo, estado.usuarioId);
    }
}
