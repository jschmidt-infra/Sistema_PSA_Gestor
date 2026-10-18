package br.unisc.psagestor.comum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface EntidadeRepository<T, ID>
        extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
    T obter(ID id);

    T bloquear(ID id);

    T salvarEAtualizar(T entidade);
}
