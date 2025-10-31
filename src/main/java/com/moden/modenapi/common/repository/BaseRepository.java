package com.moden.modenapi.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/**
 * ✅ BaseRepository
 * Generic repository interface with soft-delete aware methods.
 * All entity repositories can extend this to inherit the same logic.
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {

    /**
     * Returns only one active (non-deleted) record by ID.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<T> findActiveById(@Param("id") ID id);

    /**
     * Returns all active (non-deleted) records.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.deletedAt IS NULL")
    List<T> findAllActive();
}
