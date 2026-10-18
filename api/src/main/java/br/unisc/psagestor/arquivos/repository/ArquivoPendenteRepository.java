package br.unisc.psagestor.arquivos.repository;

import br.unisc.psagestor.arquivos.model.ArquivoPendente;
import br.unisc.psagestor.comum.repository.EntidadeRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ArquivoPendenteRepository extends EntidadeRepository<ArquivoPendente, String> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
            value =
                    "INSERT INTO arquivo_pendente(chave,criado_em) VALUES (?1,CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE criado_em=CURRENT_TIMESTAMP",
            nativeQuery = true)
    void registrar(String chave);

    List<ArquivoPendente> findTop100ByCriadoEmBeforeOrderByCriadoEmAsc(LocalDateTime limite);
}
