package br.unisc.psagestor.cache.service;

import br.unisc.psagestor.cache.model.IndicadoresNaoFinanceiros;
import java.time.Duration;
import java.util.Optional;
import java.util.function.LongSupplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true")
public class RedisIndicadores implements CacheIndicadores {
    private static final long INTERVALO_SONDAGEM_NANOS = Duration.ofSeconds(30).toNanos();
    private static final Duration VALIDADE_CACHE = Duration.ofSeconds(60);
    private final StringRedisTemplate redis;
    private final JsonMapper json;
    private final EstadoRedis estado;
    private final LongSupplier relogio;
    private volatile long suspensoAte = Long.MIN_VALUE;
    private long verificarApos = Long.MIN_VALUE;

    @org.springframework.beans.factory.annotation.Autowired
    public RedisIndicadores(StringRedisTemplate redis, JsonMapper json, EstadoRedis estado) {
        this(redis, json, estado, System::nanoTime);
    }

    RedisIndicadores(
            StringRedisTemplate redis, JsonMapper json, EstadoRedis estado, LongSupplier relogio) {
        this.redis = redis;
        this.json = json;
        this.estado = estado;
        this.relogio = relogio;
    }

    private boolean suspenso() {
        return relogio.getAsLong() < suspensoAte;
    }

    private void falha(String operacao) {
        suspensoAte = relogio.getAsLong() + INTERVALO_SONDAGEM_NANOS;
        estado.operacao(operacao, "falha");
        estado.falha();
    }

    public Optional<IndicadoresNaoFinanceiros> consultar(String chave) {
        if (suspenso()) {
            estado.operacao("leitura", "ignorada");
            return Optional.empty();
        }
        try {
            String valor = redis.opsForValue().get(chave);
            var resultado =
                    valor == null
                            ? Optional.<IndicadoresNaoFinanceiros>empty()
                            : Optional.of(json.readValue(valor, IndicadoresNaoFinanceiros.class));
            estado.operacao("leitura", valor == null ? "ausente" : "acerto");
            estado.sucesso();
            return resultado;
        } catch (RuntimeException e) {
            falha("leitura");
            return Optional.empty();
        }
    }

    public void guardar(String chave, IndicadoresNaoFinanceiros valor) {
        if (suspenso()) {
            estado.operacao("gravacao", "ignorada");
            return;
        }
        try {
            redis.opsForValue().set(chave, json.writeValueAsString(valor), VALIDADE_CACHE);
            estado.operacao("gravacao", "sucesso");
            estado.sucesso();
        } catch (RuntimeException e) {
            falha("gravacao");
        }
    }

    /** Sondagem administrativa limitada a uma tentativa a cada 30 s. */
    public synchronized void verificarDisponibilidade() {
        long agora = relogio.getAsLong();
        if (suspenso() || agora < verificarApos) return;
        verificarApos = agora + INTERVALO_SONDAGEM_NANOS;
        try (var conexao = redis.getConnectionFactory().getConnection()) {
            if (!"PONG".equals(conexao.ping()))
                throw new IllegalStateException("Resposta de sondagem inválida.");
            estado.operacao("sondagem", "sucesso");
            estado.sucesso();
        } catch (RuntimeException e) {
            falha("sondagem");
        }
    }
}
