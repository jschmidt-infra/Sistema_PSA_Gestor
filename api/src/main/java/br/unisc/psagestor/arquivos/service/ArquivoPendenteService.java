package br.unisc.psagestor.arquivos.service;

import br.unisc.psagestor.arquivos.repository.ArquivoPendenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArquivoPendenteService {
    private final ArquivoPendenteRepository repository;

    public ArquivoPendenteService(ArquivoPendenteRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String chave) {
        repository.registrar(chave);
    }
}
