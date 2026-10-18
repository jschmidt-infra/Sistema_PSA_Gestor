package br.unisc.psagestor.comum.repository;

import br.unisc.psagestor.comum.api.FalhaApi;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

public class EntidadeRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID>
        implements EntidadeRepository<T, ID> {
    private final EntityManager em;

    public EntidadeRepositoryImpl(JpaEntityInformation<T, ?> info, EntityManager em) {
        super(info, em);
        this.em = em;
    }

    @Override
    @Transactional(readOnly = true)
    public T obter(ID id) {
        return findById(id).orElseThrow(FalhaApi::inexistente);
    }

    @Override
    @Transactional
    public T bloquear(ID id) {
        T entity = obter(id);
        // Atualiza o estado após bloquear, mesmo se a entidade já estava no contexto JPA.
        em.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
        return entity;
    }

    @Override
    @Transactional
    public T salvarEAtualizar(T entity) {
        T saved = saveAndFlush(entity);
        em.refresh(saved);
        return saved;
    }
}
