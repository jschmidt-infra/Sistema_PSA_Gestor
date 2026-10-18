package br.unisc.psagestor.cache.service;

import br.unisc.psagestor.cache.model.IndicadoresNaoFinanceiros;
import java.util.Optional;

/** Chamador deve autorizar primeiro; chave inclui versão, usuário, perfil, projeto e filtros. */
public interface CacheIndicadores {
    Optional<IndicadoresNaoFinanceiros> consultar(String chaveAutorizada);

    void guardar(String chaveAutorizada, IndicadoresNaoFinanceiros indicadores);
}
