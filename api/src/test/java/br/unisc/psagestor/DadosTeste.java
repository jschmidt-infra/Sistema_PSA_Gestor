package br.unisc.psagestor;

import java.sql.Statement;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/** SQL direto restrito à preparação e inspeção independente dos dados de teste. */
final class DadosTeste {
    private final JdbcTemplate jdbc;

    DadosTeste(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long inserir(String sql, Object... args) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement =
                            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
                    return statement;
                },
                keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }

    Map<String, Object> buscar(String tabela, long id) {
        return jdbc.queryForMap("SELECT * FROM " + tabela + " WHERE id=?", id);
    }

    long numero(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).longValue();
    }
}
