package br.unisc.psagestor.financeiro.service;

import br.unisc.psagestor.financeiro.repository.ConferenciaFinanceiraRepository;
import br.unisc.psagestor.financeiro.repository.ConferenciaFinanceiraRepository.Inconsistencia;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConferenciaFinanceira {
    private final ConferenciaFinanceiraRepository repository;

    public ConferenciaFinanceira(ConferenciaFinanceiraRepository repository) {
        this.repository = repository;
    }

    public List<Inconsistencia> verificar(Long projetoId) {
        return repository.verificar(projetoId);
    }
}
