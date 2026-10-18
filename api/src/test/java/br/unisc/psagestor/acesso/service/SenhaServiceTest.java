package br.unisc.psagestor.acesso.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.unisc.psagestor.comum.api.RegraNegocioException;
import org.junit.jupiter.api.Test;

class SenhaServiceTest {
    @Test
    void consideraBytesUtf8NoLimiteDoBcrypt() {
        assertThatCode(() -> SenhaService.validarNovaSenha("á".repeat(36)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> SenhaService.validarNovaSenha("á".repeat(37)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void rejeitaSenhaAusenteOuCurta() {
        assertThatThrownBy(() -> SenhaService.validarNovaSenha(null))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> SenhaService.validarNovaSenha("a".repeat(11)))
                .isInstanceOf(RegraNegocioException.class);
        assertThatCode(() -> SenhaService.validarNovaSenha("a".repeat(12)))
                .doesNotThrowAnyException();
    }
}
