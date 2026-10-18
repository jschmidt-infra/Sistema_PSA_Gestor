package br.unisc.psagestor.cache.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.unisc.psagestor.cache.model.IndicadoresNaoFinanceiros;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

class RedisIndicadoresTest {
    @Test
    @SuppressWarnings("unchecked")
    void registraDegradacaoERecuperacaoSemEsperarTrintaSegundosReais() {
        var redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("chave-privada"))
                .thenThrow(new IllegalStateException("senha-na-excecao"))
                .thenReturn(null);
        var metrics = new SimpleMeterRegistry();
        var estado = new EstadoRedis(metrics, true);
        var clock = new AtomicLong(1);
        var cache =
                new RedisIndicadores(
                        redis,
                        JsonMapper.builder().findAndAddModules().build(),
                        estado,
                        clock::get);
        assertThat(metrics.get("psa.cache.redis.disponivel").gauge().value()).isEqualTo(-1);
        assertThat(cache.consultar("chave-privada")).isEmpty();
        assertThat(estado.degradado()).isTrue();
        assertThat(metrics.get("psa.cache.redis.disponivel").gauge().value()).isZero();
        assertThat(cache.consultar("chave-privada")).isEmpty();
        verify(ops, times(1)).get("chave-privada");
        clock.addAndGet(Duration.ofSeconds(31).toNanos());
        assertThat(cache.consultar("chave-privada")).isEmpty();
        assertThat(estado.degradado()).isFalse();
        assertThat(metrics.get("psa.cache.redis.disponivel").gauge().value()).isEqualTo(1);
        assertThat(
                        metrics.get("psa.cache.redis.operacoes")
                                .tags("operacao", "leitura", "resultado", "falha")
                                .counter()
                                .count())
                .isEqualTo(1);
        assertThat(estado.detalhes()).containsKey("ultimaFalha");
    }

    @Test
    void sondagemDetectaFalhaAntesDoPrimeiroUsoERetomaSemInterromperCache() {
        var redis = mock(StringRedisTemplate.class);
        var factory = mock(RedisConnectionFactory.class);
        var connection = mock(RedisConnection.class);
        when(redis.getConnectionFactory()).thenReturn(factory);
        when(factory.getConnection())
                .thenThrow(new IllegalStateException("offline"))
                .thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        var estado = new EstadoRedis(new SimpleMeterRegistry(), true);
        var clock = new AtomicLong(1);
        var cache =
                new RedisIndicadores(
                        redis,
                        JsonMapper.builder().findAndAddModules().build(),
                        estado,
                        clock::get);
        cache.verificarDisponibilidade();
        cache.verificarDisponibilidade();
        assertThat(estado.degradado()).isTrue();
        verify(factory, times(1)).getConnection();
        clock.addAndGet(Duration.ofSeconds(31).toNanos());
        cache.verificarDisponibilidade();
        assertThat(estado.degradado()).isFalse();
        verify(connection).close();
    }

    @Test
    void falhaDoRedisNaoImpedeConsultaNemRepeteConexaoImediatamente() {
        var redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new IllegalStateException("offline"));
        var cache =
                new RedisIndicadores(
                        redis,
                        JsonMapper.builder().findAndAddModules().build(),
                        new EstadoRedis(new SimpleMeterRegistry(), true));
        assertThat(cache.consultar("teste")).isEmpty();
        assertThat(cache.consultar("teste")).isEmpty();
        cache.guardar("teste", new IndicadoresNaoFinanceiros(1, BigDecimal.ONE, Instant.now()));
        verify(redis, times(1)).opsForValue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void gravaSomenteIndicadoresComExpiracaoDeSessentaSegundos() {
        var redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        var json = JsonMapper.builder().findAndAddModules().build();
        var dado =
                new IndicadoresNaoFinanceiros(
                        2, new BigDecimal("1.5000"), Instant.parse("2026-10-16T00:00:00Z"));
        var cache =
                new RedisIndicadores(redis, json, new EstadoRedis(new SimpleMeterRegistry(), true));
        cache.guardar("usuario:filtro", dado);
        verify(ops).set("usuario:filtro", json.writeValueAsString(dado), Duration.ofSeconds(60));
        when(ops.get("usuario:filtro")).thenReturn(json.writeValueAsString(dado));
        assertThat(cache.consultar("usuario:filtro")).contains(dado);
    }
}
