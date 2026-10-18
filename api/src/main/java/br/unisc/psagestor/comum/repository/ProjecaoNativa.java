package br.unisc.psagestor.comum.repository;

import br.unisc.psagestor.comum.api.Dados;
import br.unisc.psagestor.comum.api.FalhaApi;
import br.unisc.psagestor.comum.api.Pagina;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;

/** Infraestrutura de projeções; não é um bean e não oferece escrita ou busca por tabela. */
public abstract class ProjecaoNativa {
    @PersistenceContext private EntityManager em;

    protected record Campo(String nome, BasicType<?> tipo) {}

    protected static Campo campo(String nome) {
        return new Campo(nome, null);
    }

    protected static Campo dataHora(String nome) {
        return new Campo(
                nome,
                new BasicTypeImpl<>(
                        LocalDateTimeJavaType.INSTANCE, LocalDateTimeJdbcType.INSTANCE));
    }

    protected List<Map<String, Object>> linhas(String sql, Object... args) {
        return linhas(List.of(), sql, args);
    }

    private List<Map<String, Object>> linhas(List<Campo> campos, String sql, Object... args) {
        var query = em.createNativeQuery(sql, Tuple.class);
        var nativeQuery = query.unwrap(NativeQuery.class);
        for (var campo : campos) {
            if (campo.tipo() == null) nativeQuery.addScalar(campo.nome());
            else nativeQuery.addScalar(campo.nome(), campo.tipo());
        }
        for (int i = 0; i < args.length; i++) query.setParameter(i + 1, args[i]);
        @SuppressWarnings("unchecked")
        List<Tuple> rows = query.getResultList();
        return rows.stream()
                .map(
                        row -> {
                            Map<String, Object> out = new LinkedHashMap<>();
                            for (var e : row.getElements())
                                out.put(e.getAlias().toLowerCase(Locale.ROOT), row.get(e));
                            return out;
                        })
                .toList();
    }

    protected Map<String, Object> um(String sql, Object... args) {
        var rows = linhas(sql, args);
        if (rows.isEmpty()) throw FalhaApi.inexistente();
        return rows.getFirst();
    }

    protected Pagina<Map<String, Object>> pagina(String sql, int page, int size, Object... args) {
        return pagina(List.of(), sql, page, size, args);
    }

    protected Pagina<Map<String, Object>> pagina(
            List<Campo> campos, String sql, int page, int size, Object... args) {
        Pagina.validar(page, size);
        long count =
                ((Number)
                                um("SELECT COUNT(*) total FROM (" + sql + ") registros", args)
                                        .get("total"))
                        .longValue();
        var params = new ArrayList<>(Arrays.asList(args));
        params.add(size);
        params.add((long) page * size);
        return new Pagina<>(
                linhas(campos, sql + " ORDER BY id DESC LIMIT ? OFFSET ?", params.toArray())
                        .stream()
                        .map(Dados::saida)
                        .toList(),
                page,
                size,
                count);
    }
}
