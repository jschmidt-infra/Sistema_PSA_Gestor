package br.unisc.psagestor.cache.service;

import br.unisc.psagestor.cache.model.IndicadoresNaoFinanceiros;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Base inicial sem conexão Redis; consultas devem seguir para o MariaDB. */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.cache.enabled",
        havingValue = "false",
        matchIfMissing = true)
public class CacheDesativado implements CacheIndicadores {
    public Optional<IndicadoresNaoFinanceiros> consultar(String chaveAutorizada) {
        return Optional.empty();
    }

    public void guardar(String chaveAutorizada, IndicadoresNaoFinanceiros indicadores) {}
}
