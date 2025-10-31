package com.moden.modenapi.modules.consult.repository;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.consult.model.Consultation;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationRepository extends BaseRepository<Consultation, UUID> {

    Optional<Consultation> findByIdAndDeletedAtIsNull(UUID id);

    List<Consultation> findAllByServiceIdAndDeletedAtIsNull(UUID serviceId);

    List<Consultation> findAllByServiceIdAndStatusAndDeletedAtIsNull(UUID serviceId, ConsultationStatus status);

    @Query("SELECT c FROM Consultation c WHERE c.serviceId = :serviceId AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    List<Consultation> findRecentByServiceId(UUID serviceId);
}
