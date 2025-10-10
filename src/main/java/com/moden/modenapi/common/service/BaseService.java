package com.moden.modenapi.common.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/**
 * Generic base service that provides common CRUD operations.
 */
@Transactional
public abstract class BaseService<T, ID extends UUID> {

    protected abstract JpaRepository<T, ID> getRepository();

    @Transactional(readOnly = true)
    public List<T> getAll() {
        return getRepository().findAll();
    }

    @Transactional(readOnly = true)
    public T getById(ID id) {
        return getRepository().findById(id)
                .orElseThrow(() -> new RuntimeException("Entity not found"));
    }

    public T save(T entity) {
        return getRepository().save(entity);
    }

    public void delete(ID id) {
        if (!getRepository().existsById(id)) {
            throw new RuntimeException("Entity not found");
        }
        getRepository().deleteById(id);
    }
}
