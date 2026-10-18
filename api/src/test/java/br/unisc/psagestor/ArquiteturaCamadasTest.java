package br.unisc.psagestor;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Protege a fronteira HTTP contra a reintrodução de SQL, transações e persistência. */
class ArquiteturaCamadasTest {
    @Test
    void persistenciaDeProducaoNaoUsaRepositorioSqlGenericoNemJdbcDireto() throws Exception {
        try (var files = Files.walk(Path.of("src/main/java/br/unisc/psagestor"))) {
            for (var path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                assertThat(source)
                        .as("Persistência JPA: %s", path)
                        .doesNotContain("RegistroRepository", "JdbcTemplate");
                if (path.getParent().endsWith("service")) {
                    assertThat(source)
                            .as("Consultas pertencem aos repositórios: %s", path)
                            .doesNotContain("SELECT ", "INSERT INTO ", "UPDATE ", "DELETE FROM ");
                }
            }
        }
    }

    @Test
    void controllersNaoAcessamPersistenciaNemDeclaramTransacoes() throws Exception {
        try (var files = Files.walk(Path.of("src/main/java/br/unisc/psagestor"))) {
            var controllers =
                    files.filter(p -> p.getFileName().toString().endsWith("Controller.java"))
                            .toList();
            assertThat(controllers).isNotEmpty();
            var proibido =
                    Pattern.compile(
                            "RegistroRepository|JdbcTemplate|EntityManager|\\.repository\\.|@Transactional|SessaoAtual|PasswordEncoder|\\b(SELECT|INSERT|UPDATE|DELETE FROM)\\b");
            for (var path : controllers) {
                String source = Files.readString(path);
                assertThat(path.getParent().getFileName().toString())
                        .as("Pacote HTTP: %s", path)
                        .isEqualTo("api");
                assertThat(proibido.matcher(source).find())
                        .as("Controller sem acesso a dados/regras: %s", path)
                        .isFalse();
            }
        }
    }

    @Test
    void servicosERepositoriosNaoDependemDeControllersNemDeRespostasHttp() throws Exception {
        try (var files = Files.walk(Path.of("src/main/java/br/unisc/psagestor"))) {
            for (var path :
                    files.filter(
                                    p ->
                                            p.toString().endsWith(".java")
                                                    && (p.getParent().endsWith("service")
                                                            || p.getParent()
                                                                    .endsWith("repository")))
                            .toList()) {
                String source = Files.readString(path);
                assertThat(source)
                        .as("Direção das dependências: %s", path)
                        .doesNotContain(
                                "Controller;",
                                "ResponseEntity",
                                "@RestController",
                                "@GetMapping",
                                "@PostMapping");
            }
        }
    }
}
