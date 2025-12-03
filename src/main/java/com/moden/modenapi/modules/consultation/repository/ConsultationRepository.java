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


    /**
     * Customer + optional filters: status, createdAt interval
     * (soft-delete = NULL)
     */
    @Query("""
      SELECT c
      FROM Consultation c
      JOIN Reservation r ON c.reservationId = r.id
      WHERE r.customerId = :customerId
        AND c.deletedAt IS NULL
        AND (:status IS NULL OR c.status = :status)
        AND (:from IS NULL OR c.createdAt >= :from)
        AND (:to   IS NULL OR c.createdAt < :to)
      ORDER BY c.createdAt DESC
    """)
    List<Consultation> findForCustomerWithFilters(
            @Param("customerId") UUID customerId,
            @Param("status") ConsultationStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    /**
     * Dynamic filter for customers (service, reservation date, etc.) – native query version
     */
    @Query(
            value = """
      SELECT c.* FROM consultation c
      INNER JOIN reservation r ON r.id = c.reservation_id
      INNER JOIN studio_service s ON s.id = r.service_id
      WHERE r.customer_id = :customerId
        AND (:serviceId IS NULL OR r.service_id = :serviceId)
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

    @Query(
            value = """
      SELECT c.* FROM consultation c
      INNER JOIN reservation r ON r.id = c.reservation_id
      INNER JOIN studio_service s ON s.id = r.service_id
      WHERE (:designerId IS NULL OR c.designer_id = :designerId)
        AND (:customerId IS NULL OR r.customer_id = :customerId)
        AND (:serviceId  IS NULL OR r.service_id  = :serviceId)
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

    /**
     * Dynamic filter for staff: designerId, customerId, serviceId, date range, status — native version
     */
    @Query(
            value = """
      SELECT c.* FROM consultation c
      INNER JOIN reservation r ON r.id = c.reservation_id
      WHERE (:designerId IS NULL OR c.designer_id = :designerId)
        AND (:customerId IS NULL OR r.customer_id = :customerId)
        AND (:serviceId  IS NULL OR r.service_id  = :serviceId)
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
