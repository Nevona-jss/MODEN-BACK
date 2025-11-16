package com.moden.modenapi.modules.payment.repository;

import com.moden.modenapi.common.enums.PaymentStatus;
import com.moden.modenapi.common.enums.ServiceType;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.payment.model.Payment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends BaseRepository<Payment, UUID> {

    /**
     * Payment list filter:
     *  - studioId (StudioService.studioId)
     *  - designerId (Reservation.designerId)
     *  - serviceType (StudioService.serviceType)
     *  - paymentStatus (Payment.paymentStatus)
     *  - from / to (Reservation.reservationAt)
     */
    @Query("""
        select p
        from Payment p
        join Reservation r on p.reservationId = r.id
        join StudioService s on r.serviceId = s.id
        where (:studioId is null or s.studioId = :studioId)
          and (:designerId is null or r.designerId = :designerId)
          and (:serviceType is null or s.serviceType = :serviceType)
          and (:status is null or p.paymentStatus = :status)
          and (:from is null or r.reservationAt >= :from)
          and (:to is null or r.reservationAt < :to)
    """)
    List<Payment> searchPayments(
            @Param("studioId") UUID studioId,
            @Param("designerId") UUID designerId,
            @Param("serviceType") ServiceType serviceType,
            @Param("status") PaymentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
    Optional<Payment> findByReservationId(UUID reservationId);

    @Query("""
    select 
        coalesce(sum(p.totalAmount), 0),
        count(p),
        coalesce(avg(p.totalAmount), 0)
    from Payment p
    join Reservation r on p.reservationId = r.id
    join StudioService s on r.serviceId = s.id
    where (:studioId is null or s.studioId = :studioId)
      and p.paymentStatus = :status
      and p.createdAt >= :startAt
      and p.createdAt < :endAt
    """)
    List<Object[]> aggregateSalesForPeriod(
            @Param("studioId") UUID studioId,
            @Param("status") PaymentStatus status,
            @Param("startAt") java.time.Instant startAt,
            @Param("endAt") java.time.Instant endAt
    );

}
