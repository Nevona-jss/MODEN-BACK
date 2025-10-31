package com.moden.modenapi.modules.studioservice.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.studioservice.model.StudioService;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudioServiceRepository extends BaseRepository<StudioService, UUID> {

    @Query("SELECT s FROM StudioService s WHERE s.studioId = :studioId AND s.deletedAt IS NULL ORDER BY s.createdAt DESC")
    List<StudioService> findAllActiveByStudioId(UUID studioId);

    @Query("SELECT s FROM StudioService s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<StudioService> findActiveById(UUID id);

    Optional<StudioService> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
    SELECT s FROM StudioService s 
    WHERE s.studioId = :studioId 
      AND s.reservedDate = :date
      AND s.deletedAt IS NULL 
    ORDER BY s.startAt ASC
""")
    List<StudioService> findAllByStudioIdAndDate(UUID studioId, LocalDate date);

    @Query("""
    SELECT s FROM StudioService s 
    WHERE s.studioId = :studioId 
      AND s.reservedDate BETWEEN :startDate AND :endDate
      AND s.deletedAt IS NULL 
    ORDER BY s.startAt ASC
""")
    List<StudioService> findAllByStudioIdAndDateRange(UUID studioId, LocalDate startDate, LocalDate endDate);

}
