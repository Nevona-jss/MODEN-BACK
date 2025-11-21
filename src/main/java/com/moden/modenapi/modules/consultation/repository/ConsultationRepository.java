package com.moden.modenapi.modules.consultation.repository;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.consultation.model.Consultation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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



    /**
     * 현재 로그인한 고객 기준 상담 목록 동적 조회 (native query)
     * - customerId: 필수 (항상 내 것만)
     * - serviceId: 옵션 (null이면 전체)
     * - from: 옵션 (null이면 시작 제한 없음)
     * - to: 옵션 (null이면 끝 제한 없음, [from, to) 범위)
     */
    @Query(
            value = """
            SELECT c.*
            FROM consultation c
            JOIN reservation r ON r.id = c.reservation_id
            WHERE r.customer_id = :customerId
              AND (:serviceId IS NULL OR r.service_id = :serviceId)
              AND (:from IS NULL OR r.reservation_at >= :from)
              AND (:to   IS NULL OR r.reservation_at < :to)
              AND (c.deleted_at IS NULL)
            ORDER BY r.reservation_at DESC, c.created_at DESC
            """,
            nativeQuery = true
    )
    List<Consultation> searchDynamicForCustomer(
            @Param("customerId") UUID customerId,
            @Param("serviceId") UUID serviceId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query(
            value = """
        SELECT c.*
        FROM consultation c
        JOIN reservation r ON r.id = c.reservation_id
        WHERE (:designerId IS NULL OR r.designer_id = :designerId)
          AND (:customerId IS NULL OR r.customer_id = :customerId)
          AND (:serviceId  IS NULL OR r.service_id  = :serviceId)
          AND (:status     IS NULL OR c.status      = :status)
          AND (:from       IS NULL OR r.reservation_at >= :from)
          AND (:to         IS NULL OR r.reservation_at <  :to)
          AND (c.deleted_at IS NULL)
        ORDER BY r.reservation_at DESC, c.created_at DESC
        """,
            nativeQuery = true
    )
    List<Consultation> searchDynamicForStaff(
            @Param("designerId") UUID designerId,
            @Param("customerId") UUID customerId,
            @Param("serviceId") UUID serviceId,
            @Param("status") ConsultationStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );


}
