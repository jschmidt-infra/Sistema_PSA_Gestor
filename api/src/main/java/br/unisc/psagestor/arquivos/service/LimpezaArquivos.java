package br.unisc.psagestor.arquivos.service;

import br.unisc.psagestor.arquivos.model.ArquivoPendente;
import br.unisc.psagestor.arquivos.repository.ArquivoPendenteRepository;
import br.unisc.psagestor.arquivos.repository.CroquiRepository;
import br.unisc.psagestor.contratos.repository.ContratoRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class LimpezaArquivos {
    private static final Logger LOG = LoggerFactory.getLogger(LimpezaArquivos.class);
    private static final long CARENCIA_SEGUNDOS = 3600;
    private final ArquivoPendenteRepository pendentes;
    private final CroquiRepository croquis;
    private final ContratoRepository contratos;
    private final ArquivoStorage storage;

    public LimpezaArquivos(
            ArquivoPendenteRepository pendentes,
            CroquiRepository croquis,
            ContratoRepository contratos,
            ArquivoStorage storage) {
        this.pendentes = pendentes;
        this.croquis = croquis;
        this.contratos = contratos;
        this.storage = storage;
    }

    @Scheduled(
            fixedDelayString = "${app.storage.cleanup-delay-ms:600000}",
            initialDelayString = "${app.storage.cleanup-delay-ms:600000}")
    public void limpar() {
        try {
            var chaves =
                    pendentes
                            .findTop100ByCriadoEmBeforeOrderByCriadoEmAsc(
                                    LocalDateTime.now().minusSeconds(CARENCIA_SEGUNDOS))
                            .stream()
                            .map(ArquivoPendente::getChave)
                            .toList();
            for (String chave : chaves) {
                try {
                    // Referências são conferidas novamente antes de excluir um objeto candidato.
                    long usados =
                            croquis.countByChave(chave) + contratos.countByDocumentoChave(chave);
                    if (usados == 0) storage.removerObjetoNovo(chave);
                    pendentes.deleteById(chave);
                } catch (RuntimeException e) {
                    LOG.warn("Limpeza de arquivo adiada; nova tentativa no próximo ciclo.");
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("Consulta de arquivos pendentes indisponível.");
        }
    }
}
