package br.unisc.psagestor.cache.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("cacheRedis")
public class CacheRedisHealthIndicator implements HealthIndicator {
    private final EstadoRedis estado;
    private final ObjectProvider<RedisIndicadores> redis;

    public CacheRedisHealthIndicator(EstadoRedis estado, ObjectProvider<RedisIndicadores> redis) {
        this.estado = estado;
        this.redis = redis;
    }

    @Override
    public Health health() {
        var cache = redis.getIfAvailable();
        if (cache != null) cache.verificarDisponibilidade();
        return Health.status(estado.degradado() ? "DEGRADED" : "UP")
                .withDetails(estado.detalhes())
                .build();
    }
}
