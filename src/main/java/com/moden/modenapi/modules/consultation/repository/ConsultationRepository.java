package com.moden.modenapi.modules.consultation.repository;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.consultation.model.Consultation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationRepository extends BaseRepository<Consultation, UUID> {

    // 예약 ID 기준 단건 조회 (1:1 이라고 가정)
    Optional<Consultation> findByReservationId(UUID reservationId);

    // 상담 상태별 목록 조회 (상담대기 / 상담완료 등)
    List<Consultation> findByStatus(ConsultationStatus status);

    // 여러 예약 ID 에 대한 상담 목록 (디자이너별 조회 등에 활용 가능)
    List<Consultation> findByReservationIdIn(List<UUID> reservationIds);
}
