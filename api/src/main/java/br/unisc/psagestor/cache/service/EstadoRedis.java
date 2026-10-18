package br.unisc.psagestor.cache.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Estado operacional local; não armazena chaves, valores ou credenciais do cache. */
@Component
public class EstadoRedis {
    private static final Logger LOG = LoggerFactory.getLogger(EstadoRedis.class);
    private final MeterRegistry meters;
    private final boolean habilitado;
    private volatile int disponibilidade = -1;
    private Instant ultimaVerificacao;
    private Instant ultimaFalha;

    public EstadoRedis(
            MeterRegistry meters, @Value("${app.cache.enabled:false}") boolean habilitado) {
        this.meters = meters;
        this.habilitado = habilitado;
        meters.gauge("psa.cache.redis.habilitado", this, s -> s.habilitado ? 1 : 0);
        meters.gauge("psa.cache.redis.disponivel", this, s -> s.disponibilidade);
    }

    public boolean degradado() {
        return habilitado && disponibilidade == 0;
    }

    public void operacao(String operacao, String resultado) {
        meters.counter("psa.cache.redis.operacoes", "operacao", operacao, "resultado", resultado)
                .increment();
    }

    public synchronized void sucesso() {
        boolean recuperado = disponibilidade == 0;
        disponibilidade = 1;
        ultimaVerificacao = Instant.now();
        if (recuperado) LOG.info("cache_redis estado=RECUPERADO");
    }

    public synchronized void falha() {
        boolean mudou = disponibilidade != 0;
        disponibilidade = 0;
        ultimaVerificacao = Instant.now();
        ultimaFalha = ultimaVerificacao;
        if (mudou)
            LOG.warn("cache_redis estado=DEGRADADO fallback=MariaDB novaTentativaSegundos=30");
    }

    public synchronized Map<String, Object> detalhes() {
        var dados = new LinkedHashMap<String, Object>();
        dados.put("habilitado", habilitado);
        dados.put(
                "estado",
                !habilitado
                        ? "DESATIVADO"
                        : disponibilidade < 0
                                ? "NAO_VERIFICADO"
                                : disponibilidade == 0 ? "INDISPONIVEL" : "DISPONIVEL");
        dados.put("fallback", "MariaDB");
        if (ultimaVerificacao != null) dados.put("ultimaVerificacao", ultimaVerificacao.toString());
        if (ultimaFalha != null) dados.put("ultimaFalha", ultimaFalha.toString());
        return dados;
    }
}
