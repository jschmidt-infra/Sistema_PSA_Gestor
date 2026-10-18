package br.unisc.psagestor.comum.api;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Adapta projeções do banco ao contrato JSON sem expor entidades JPA. */
public final class Dados {
    private Dados() {}

    public static long numero(Map<String, Object> registro, String campo) {
        return ((Number) registro.get(campo)).longValue();
    }

    public static BigDecimal decimal(Map<String, Object> registro, String campo) {
        return (BigDecimal) registro.get(campo);
    }

    public static Map<String, Object> saida(Map<String, Object> registro) {
        var resultado = new LinkedHashMap<String, Object>();
        registro.forEach((campo, valor) -> resultado.put(camelCase(campo), valorJson(valor)));
        return resultado;
    }

    private static String camelCase(String campo) {
        String[] partes = campo.toLowerCase(Locale.ROOT).split("_");
        var nome = new StringBuilder(partes[0]);
        for (int indice = 1; indice < partes.length; indice++) {
            nome.append(Character.toUpperCase(partes[indice].charAt(0)))
                    .append(partes[indice].substring(1));
        }
        return nome.toString();
    }

    private static Object valorJson(Object valor) {
        // Decimais são strings no contrato HTTP para não perder precisão em clientes JavaScript.
        if (valor instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        // Preserva a interpretação dos DATETIME já utilizada pelo mapeamento JDBC/JPA do projeto.
        if (valor instanceof LocalDateTime dataHora) {
            return Timestamp.valueOf(dataHora).toInstant().toString();
        }
        if (valor instanceof Timestamp instante) {
            return instante.toInstant().toString();
        }
        if (valor instanceof Date data) {
            return data.toLocalDate().toString();
        }
        return valor;
    }
}
