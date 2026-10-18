package br.unisc.psagestor.cache.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.unisc.psagestor.cache.model.IndicadoresNaoFinanceiros;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Teste opt-in: usa somente uma chave exclusiva e não acessa o MariaDB. */
@EnabledIfEnvironmentVariable(named = "PSA_REDIS_LIVE_TEST", matches = "true")
class RedisLiveTest {
    @Test
    void autenticaGravaLeEConfirmaTtlNoRedisLocal() {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource("config/application-local.yml"));
        var props = yaml.getObject();
        assertThat(props).isNotNull();
        assertThat(props.getProperty("app.cache.enabled")).isEqualTo("true");
        var server =
                new RedisStandaloneConfiguration(
                        props.getProperty("spring.data.redis.host"),
                        Integer.parseInt(props.getProperty("spring.data.redis.port")));
        server.setPassword(props.getProperty("spring.data.redis.password"));
        var client =
                LettuceClientConfiguration.builder().commandTimeout(Duration.ofMillis(200)).build();
        var factory = new LettuceConnectionFactory(server, client);
        factory.afterPropertiesSet();
        factory.start();
        var redis = new StringRedisTemplate(factory);
        String key = "psa:testes:redis:" + UUID.randomUUID();
        try {
            try (var connection = factory.getConnection()) {
                assertThat(connection.ping()).isEqualTo("PONG");
            }
            new ApplicationContextRunner()
                    .withUserConfiguration(RedisIndicadores.class, CacheDesativado.class)
                    .withBean(StringRedisTemplate.class, () -> redis)
                    .withBean(
                            EstadoRedis.class,
                            () -> new EstadoRedis(new SimpleMeterRegistry(), true))
                    .withBean(
                            JsonMapper.class,
                            () -> JsonMapper.builder().findAndAddModules().build())
                    .withPropertyValues("app.cache.enabled=true")
                    .run(
                            context -> {
                                assertThat(context).hasSingleBean(CacheIndicadores.class);
                                var cache = context.getBean(CacheIndicadores.class);
                                assertThat(cache).isInstanceOf(RedisIndicadores.class);
                                assertThat(cache.consultar(key)).isEmpty();
                                var value =
                                        new IndicadoresNaoFinanceiros(
                                                2, new BigDecimal("1.5000"), Instant.now());
                                cache.guardar(key, value);
                                assertThat(redis.getExpire(key)).isBetween(1L, 60L);
                                assertThat(cache.consultar(key)).contains(value);
                                assertThat(redis.delete(key)).isTrue();
                                assertThat(cache.consultar(key)).isEmpty();
                            });
        } finally {
            try {
                redis.delete(key);
            } finally {
                factory.destroy();
            }
        }
    }
}
