package br.unisc.psagestor.arquivos.service;

import static org.assertj.core.api.Assertions.*;

import br.unisc.psagestor.comum.api.FalhaApi;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class StorageConfigTest {
    @Test
    void armazenamentoNaoConfiguradoFalhaSemExporCredenciais() {
        try (var storage = new StorageConfig().r2(new MockEnvironment())) {
            assertThatThrownBy(() -> storage.abrir("teste"))
                    .isInstanceOf(FalhaApi.class)
                    .hasMessageContaining("não configurado");
        }
    }

    @Test
    void clienteS3EnviaAssinaturaEArmazenaBaixaERemoveObjeto() throws Exception {
        var objetos = new ConcurrentHashMap<String, byte[]>();
        var assinado = new AtomicBoolean();
        var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext(
                "/",
                exchange -> {
                    try {
                        assinado.set(
                                exchange.getRequestHeaders().getFirst("Authorization") != null);
                        String path = exchange.getRequestURI().getPath();
                        switch (exchange.getRequestMethod()) {
                            case "PUT" -> {
                                objetos.put(path, exchange.getRequestBody().readAllBytes());
                                exchange.sendResponseHeaders(200, -1);
                            }
                            case "GET" -> {
                                byte[] bytes = objetos.get(path);
                                exchange.sendResponseHeaders(200, bytes.length);
                                exchange.getResponseBody().write(bytes);
                            }
                            case "DELETE" -> {
                                objetos.remove(path);
                                exchange.sendResponseHeaders(204, -1);
                            }
                            default -> exchange.sendResponseHeaders(405, -1);
                        }
                    } finally {
                        exchange.close();
                    }
                });
        servidor.start();
        try {
            var env =
                    new MockEnvironment()
                            .withProperty(
                                    "app.r2.endpoint",
                                    "http://127.0.0.1:" + servidor.getAddress().getPort())
                            .withProperty("app.r2.bucket", "teste")
                            .withProperty("app.r2.access-key", "chave-de-teste")
                            .withProperty("app.r2.secret-key", "segredo-de-teste");
            try (var storage = new StorageConfig().r2(env)) {
                byte[] bytes = "arquivo de teste".getBytes(StandardCharsets.UTF_8);
                storage.guardar(
                        "objeto", new ByteArrayInputStream(bytes), bytes.length, "text/plain");
                try (var input = storage.abrir("objeto")) {
                    assertThat(input.readAllBytes()).isEqualTo(bytes);
                }
                assertThat(assinado).isTrue();
                storage.removerObjetoNovo("objeto");
                assertThat(objetos).isEmpty();
            }
        } finally {
            servidor.stop(0);
        }
    }
}
