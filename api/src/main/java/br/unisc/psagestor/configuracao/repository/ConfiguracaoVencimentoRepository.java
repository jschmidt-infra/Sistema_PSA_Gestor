package br.unisc.psagestor.configuracao.repository;

import br.unisc.psagestor.configuracao.model.ConfiguracaoVencimento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoVencimentoRepository
        extends JpaRepository<ConfiguracaoVencimento, Long> {}
