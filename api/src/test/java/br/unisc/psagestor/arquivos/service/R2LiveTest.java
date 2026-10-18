package br.unisc.psagestor.arquivos.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.env.MockEnvironment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** Teste externo opt-in: não altera registros do banco ou objetos existentes. */
@EnabledIfEnvironmentVariable(named = "PSA_R2_LIVE_TEST", matches = "true")
class R2LiveTest {
    @Test
    void gravaLeConfereConteudoERemoveSomenteObjetoTemporario() throws Exception {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource("config/application-r2-local.yml"));
        var propriedades = Objects.requireNonNull(yaml.getObject());
        var env = new MockEnvironment();
        for (String nome : propriedades.stringPropertyNames()) {
            env.setProperty(nome, propriedades.getProperty(nome));
        }
        String chave = "testes-integracao/" + UUID.randomUUID() + ".txt";
        String bucket = env.getRequiredProperty("app.r2.bucket");
        var storage = new StorageConfig().r2(env);
        byte[] esperado =
                "PSA Gestor: teste temporário de integração R2.\n".getBytes(StandardCharsets.UTF_8);
        try (var verificacao =
                S3Client.builder()
                        .endpointOverride(URI.create(env.getRequiredProperty("app.r2.endpoint")))
                        .region(Region.of("auto"))
                        .credentialsProvider(
                                StaticCredentialsProvider.create(
                                        AwsBasicCredentials.create(
                                                env.getRequiredProperty("app.r2.access-key"),
                                                env.getRequiredProperty("app.r2.secret-key"))))
                        .httpClientBuilder(
                                UrlConnectionHttpClient.builder()
                                        .connectionTimeout(Duration.ofSeconds(5))
                                        .socketTimeout(Duration.ofSeconds(15)))
                        .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(25)))
                        .build()) {
            try {
                storage.guardar(
                        chave,
                        new ByteArrayInputStream(esperado),
                        esperado.length,
                        "text/plain; charset=utf-8");
                try (var recebido = storage.abrir(chave)) {
                    assertThat(recebido.readAllBytes()).isEqualTo(esperado);
                }
                var metadata = verificacao.headObject(r -> r.bucket(bucket).key(chave));
                assertThat(metadata.contentLength()).isEqualTo((long) esperado.length);
                assertThat(metadata.contentType()).isEqualTo("text/plain; charset=utf-8");
            } finally {
                // A chave é exclusiva desta execução; nunca remove arquivos preexistentes.
                storage.removerObjetoNovo(chave);
            }
            boolean removido = false;
            try {
                verificacao.headObject(r -> r.bucket(bucket).key(chave));
            } catch (S3Exception ex) {
                assertThat(ex.statusCode()).isEqualTo(404);
                removido = true;
            }
            assertThat(removido).isTrue();
        }
    }
}
