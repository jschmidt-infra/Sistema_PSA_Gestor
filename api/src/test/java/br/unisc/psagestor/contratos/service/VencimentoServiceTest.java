package br.unisc.psagestor.contratos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import br.unisc.psagestor.configuracao.service.ConfiguracaoVencimentoService;
import java.time.*;
import org.junit.jupiter.api.Test;

class VencimentoServiceTest {
    @Test
    void usaDiaDeSaoPauloMesmoQuandoUtcJaMudou() {
        var configuracao = mock(ConfiguracaoVencimentoService.class);
        when(configuracao.consultar()).thenReturn(new ConfiguracaoVencimentoService.Saida(30));
        var relogio = Clock.fixed(Instant.parse("2026-10-19T02:59:59Z"), ZoneOffset.UTC);
        assertThat(new VencimentoService(configuracao, relogio).contexto().hoje())
                .isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(
                        new VencimentoService(
                                        configuracao, Clock.offset(relogio, Duration.ofSeconds(1)))
                                .contexto()
                                .hoje())
                .isEqualTo(LocalDate.of(2026, 10, 19));
    }
}
