package br.unisc.psagestor.arquivos.service;

import br.unisc.psagestor.comum.api.FalhaApi;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class StorageConfig {
    @Bean
    @Profile("test & !prod & !r2")
    ArquivoStorage local(Environment env) {
        Path raiz =
                Path.of(env.getProperty("app.storage.directory", "./data/arquivos"))
                        .toAbsolutePath()
                        .normalize();
        return new ArquivoStorage() {
            private Path caminho(String chave) {
                Path p = raiz.resolve(chave).normalize();
                if (!p.startsWith(raiz)) throw new IllegalArgumentException("Chave inválida.");
                return p;
            }

            @Override
            public void guardar(String chave, InputStream input, long tamanho, String mime) {
                try {
                    Files.createDirectories(raiz);
                    try (var out =
                            Files.newOutputStream(caminho(chave), StandardOpenOption.CREATE_NEW)) {
                        input.transferTo(out);
                    }
                } catch (IOException e) {
                    throw indisponivel();
                }
            }

            @Override
            public InputStream abrir(String chave) {
                try {
                    return Files.newInputStream(caminho(chave));
                } catch (IOException e) {
                    throw indisponivel();
                }
            }

            @Override
            public void removerObjetoNovo(String chave) {
                try {
                    Files.deleteIfExists(caminho(chave));
                } catch (IOException e) {
                    throw indisponivel();
                }
            }
        };
    }

    @Bean
    @Profile("r2 | !test")
    ArquivoStorage r2(Environment env) {
        return new ArquivoStorage() {
            private volatile S3Client client;

            /** Libera o cliente HTTP quando o contexto Spring é encerrado. */
            @Override
            public synchronized void close() {
                if (client != null) {
                    client.close();
                }
            }

            private String obrigatorio(String key) {
                String value = env.getProperty(key);
                if (value == null || value.isBlank()) throw indisponivel();
                return value;
            }

            // Inicialização tardia: a falta de R2 não impede autenticação e consultas sem arquivos.
            private synchronized S3Client client() {
                if (client == null)
                    client =
                            S3Client.builder()
                                    .endpointOverride(URI.create(obrigatorio("app.r2.endpoint")))
                                    .region(Region.of("auto"))
                                    .credentialsProvider(
                                            StaticCredentialsProvider.create(
                                                    AwsBasicCredentials.create(
                                                            obrigatorio("app.r2.access-key"),
                                                            obrigatorio("app.r2.secret-key"))))
                                    .serviceConfiguration(
                                            S3Configuration.builder()
                                                    .chunkedEncodingEnabled(false)
                                                    .build())
                                    .requestChecksumCalculation(
                                            RequestChecksumCalculation.WHEN_REQUIRED)
                                    .responseChecksumValidation(
                                            ResponseChecksumValidation.WHEN_REQUIRED)
                                    .httpClientBuilder(
                                            UrlConnectionHttpClient.builder()
                                                    .connectionTimeout(Duration.ofSeconds(3))
                                                    .socketTimeout(Duration.ofSeconds(15)))
                                    .overrideConfiguration(
                                            c -> c.apiCallTimeout(Duration.ofSeconds(25)))
                                    .build();
                return client;
            }

            @Override
            public void guardar(String chave, InputStream input, long tamanho, String mime) {
                try {
                    client().putObject(
                                    r ->
                                            r.bucket(obrigatorio("app.r2.bucket"))
                                                    .key(chave)
                                                    .contentType(mime),
                                    RequestBody.fromInputStream(input, tamanho));
                } catch (RuntimeException e) {
                    throw indisponivel();
                }
            }

            @Override
            public InputStream abrir(String chave) {
                try {
                    return client().getObject(
                                    r -> r.bucket(obrigatorio("app.r2.bucket")).key(chave));
                } catch (RuntimeException e) {
                    throw indisponivel();
                }
            }

            @Override
            public void removerObjetoNovo(String chave) {
                try {
                    client().deleteObject(r -> r.bucket(obrigatorio("app.r2.bucket")).key(chave));
                } catch (RuntimeException e) {
                    throw indisponivel();
                }
            }
        };
    }

    private static FalhaApi indisponivel() {
        return new FalhaApi(
                503,
                "ARMAZENAMENTO_INDISPONIVEL",
                "Armazenamento temporariamente indisponível ou não configurado.");
    }
}
