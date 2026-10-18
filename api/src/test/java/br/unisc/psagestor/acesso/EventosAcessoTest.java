package br.unisc.psagestor.acesso;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.unisc.psagestor.acesso.repository.EventoAcessoRepository;
import br.unisc.psagestor.acesso.service.EventosAcesso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(OutputCaptureExtension.class)
class EventosAcessoTest {
    @Test
    void falhaDePersistenciaNaoImpedeEncerramentoENaoExpoeExcecao(CapturedOutput output) {
        var jdbc = mock(EventoAcessoRepository.class);
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
        doThrow(new DataAccessResourceFailureException("SEGREDO-NA-EXCECAO"))
                .when(jdbc)
                .registrar(any(), any(), any(), any());
        var eventos = new EventosAcesso(jdbc, manager);
        var session = new MockHttpSession(null, "COOKIE-SECRETO-NAO-REGISTRAR");
        assertThatCode(
                        () -> {
                            eventos.iniciar(session, 123L);
                            eventos.encerrar(session, EventosAcesso.Motivo.LOGOUT);
                            eventos.encerrar(
                                    session,
                                    EventosAcesso.Motivo.EXPIRACAO_OU_ENCERRAMENTO_CONTAINER);
                            session.invalidate();
                        })
                .doesNotThrowAnyException();
        assertThat(session.isInvalid()).isTrue();
        verify(jdbc, times(2)).registrar(any(), any(), any(), any());
        assertThat(output.getAll())
                .contains("persistencia=indisponivel", "SESSAO_ENCERRADA", "LOGOUT")
                .doesNotContain("SEGREDO-NA-EXCECAO", "COOKIE-SECRETO-NAO-REGISTRAR");
    }
}
