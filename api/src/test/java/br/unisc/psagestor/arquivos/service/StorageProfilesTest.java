package br.unisc.psagestor.arquivos.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class StorageProfilesTest {
    private final ApplicationContextRunner contexto =
            new ApplicationContextRunner().withUserConfiguration(StorageConfig.class);

    @Test
    void demonstracaoContinuaLocalSemAtivacaoExplicitaDoR2() {
        contexto.withPropertyValues("spring.profiles.active=test")
                .run(
                        c -> {
                            assertThat(c).hasSingleBean(ArquivoStorage.class);
                            assertThat(c).hasBean("local").doesNotHaveBean("r2");
                        });
    }

    @Test
    void demonstracaoComR2UsaUmUnicoAdaptadorRemoto() {
        contexto.withPropertyValues("spring.profiles.active=demonstracao,r2")
                .run(
                        c -> {
                            assertThat(c).hasSingleBean(ArquivoStorage.class);
                            assertThat(c).hasBean("r2").doesNotHaveBean("local");
                        });
    }
}
