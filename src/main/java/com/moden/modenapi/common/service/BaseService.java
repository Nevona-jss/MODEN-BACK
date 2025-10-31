package com.moden.modenapi.common.service;

import com.moden.modenapi.common.model.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Generic base service providing common CRUD operations.
 */
@Transactional
public abstract class BaseService<T> {

    protected abstract JpaRepository<T, UUID> getRepository();

    public T create(T entity) {
        return getRepository().save(entity);
    }

    @Transactional(readOnly = true)
    public T readById(UUID id) {
        return getRepository().findById(id)
                .orElseThrow(() -> new RuntimeException("Entity not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<T> readAll() {
        return getRepository().findAll();
    }

    public T update(T entity) {
        return getRepository().save(entity);
    }

    @Transactional
    public void softDelete(UUID id) {
        var repo = getRepository();
        T entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entity not found"));
        if (entity instanceof BaseEntity be) {
            be.setDeletedAt(Instant.now());
            repo.save(entity);
        } else {
            repo.deleteById(id);
        }
    }

}
