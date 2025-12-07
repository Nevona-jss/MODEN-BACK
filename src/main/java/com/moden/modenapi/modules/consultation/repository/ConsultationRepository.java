package com.moden.modenapi.modules.consultation.repository;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.consultation.model.Consultation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationRepository extends BaseRepository<Consultation, UUID> {

    Optional<Consultation> findByReservationId(UUID reservationId);

    List<Consultation> findByReservationIdIn(List<UUID> reservationIds);

    // =========================================================
    // 1) 스튜디오/고객 기준 리스트 + (옵션) serviceId 필터
    //    - Reservation 이 serviceIds (ElementCollection) 로 바뀐 버전
    // =========================================================
    @Query(
            value = """
        SELECT c.*
        FROM consultation c
        INNER JOIN reservation r
            ON r.id = c.reservation_id
        LEFT JOIN reservation_service_ids rs
            ON rs.reservation_id = r.id
        WHERE r.customer_id = :customerId
          AND (:serviceId IS NULL OR rs.service_id = :serviceId)
          AND (:status   IS NULL OR c.status = :status)
          AND (:from     IS NULL OR c.created_at >= :from)
          AND (:to       IS NULL OR c.created_at <  :to)
          AND c.deleted_at IS NULL
        """,
            nativeQuery = true
    )
    List<Consultation> findForCustomerWithFilters(
            @Param("customerId") UUID customerId,
            @Param("serviceId") UUID serviceId,
            @Param("status") ConsultationStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    // =========================================================
    // 2) 고객용 동적 검색 (serviceId / serviceNameKeyword / date)
    //    Reservation.serviceIds + studio_service 조인
    // =========================================================
    @Query(
            value = """
      SELECT c.*
      FROM consultation c
      INNER JOIN reservation r
        ON r.id = c.reservation_id
      INNER JOIN reservation_service_ids rs
        ON rs.reservation_id = r.id
      INNER JOIN studio_service s
        ON s.id = rs.service_id
      WHERE r.customer_id = :customerId
        AND (:serviceId IS NULL OR rs.service_id = :serviceId)
        AND (:serviceNameKeyword IS NULL OR s.service_name LIKE '%' + :serviceNameKeyword + '%')
        AND (:fromDate IS NULL OR r.reservation_date >= :fromDate)
        AND (:toDate   IS NULL OR r.reservation_date <  :toDate)
        AND c.deleted_at IS NULL
      ORDER BY r.reservation_date DESC, r.start_time DESC, c.created_at DESC
      """,
            nativeQuery = true
    )
    List<Consultation> searchDynamicForCustomer(
            @Param("customerId") UUID customerId,
            @Param("serviceId") UUID serviceId,
            @Param("serviceNameKeyword") String serviceNameKeyword,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    // =========================================================
    // 3) 직원용 동적 검색 (서비스명 + 기타 필터)
    //    Reservation.serviceIds + studio_service 조인
    // =========================================================
    @Query(
            value = """
      SELECT c.*
      FROM consultation c
      INNER JOIN reservation r
        ON r.id = c.reservation_id
      INNER JOIN reservation_service_ids rs
        ON rs.reservation_id = r.id
      INNER JOIN studio_service s
        ON s.id = rs.service_id
      WHERE (:designerId IS NULL OR c.designer_id = :designerId)
        AND (:customerId IS NULL OR r.customer_id = :customerId)
        AND (:serviceId  IS NULL OR rs.service_id  = :serviceId)
        AND (:serviceNameKeyword IS NULL OR s.service_name LIKE '%' + :serviceNameKeyword + '%')
        AND (:status     IS NULL OR c.status = :status)
        AND (:fromDate   IS NULL OR r.reservation_date >= :fromDate)
        AND (:toDate     IS NULL OR r.reservation_date <  :toDate)
        AND c.deleted_at IS NULL
      ORDER BY r.reservation_date DESC, r.start_time DESC, c.created_at DESC
      """,
            nativeQuery = true
    )
    List<Consultation> searchDynamicForStaff(
            @Param("designerId") UUID designerId,
            @Param("customerId") UUID customerId,
            @Param("serviceId") UUID serviceId,
            @Param("serviceNameKeyword") String serviceNameKeyword,
            @Param("status") ConsultationStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    // =========================================================
    // 4) 직원용 동적 검색 (서비스명 없이, serviceId / 날짜 / status 만)
    // =========================================================
    @Query(
            value = """
      SELECT c.*
      FROM consultation c
      INNER JOIN reservation r
        ON r.id = c.reservation_id
      LEFT JOIN reservation_service_ids rs
        ON rs.reservation_id = r.id
      WHERE (:designerId IS NULL OR c.designer_id = :designerId)
        AND (:customerId IS NULL OR r.customer_id = :customerId)
        AND (:serviceId  IS NULL OR rs.service_id  = :serviceId)
        AND (:status     IS NULL OR c.status = :status)
        AND (:fromDate   IS NULL OR r.reservation_date >= :fromDate)
        AND (:toDate     IS NULL OR r.reservation_date <  :toDate)
        AND c.deleted_at IS NULL
      ORDER BY r.reservation_date DESC, r.start_time DESC, c.created_at DESC
      """,
            nativeQuery = true
    )
    List<Consultation> searchDynamicForStaff(
            @Param("designerId") UUID designerId,
            @Param("customerId") UUID customerId,
            @Param("serviceId") UUID serviceId,
            @Param("status") ConsultationStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

}
