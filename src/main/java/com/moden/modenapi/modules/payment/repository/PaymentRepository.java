package com.moden.modenapi.modules.payment.repository;

import com.moden.modenapi.common.enums.PaymentStatus;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.payment.model.Payment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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
          and (:serviceName is null or s.serviceName = :serviceName)
          and (:status is null or p.paymentStatus = :status)
          and (:fromDate is null or r.reservationDate >= :fromDate)
          and (:toDate   is null or r.reservationDate <= :toDate)
        """)
    List<Payment> searchPayments(
            @Param("studioId")   UUID studioId,
            @Param("designerId") UUID designerId,
            @Param("serviceName") String serviceName,
            @Param("status")     PaymentStatus status,
            @Param("fromDate")   LocalDate fromDate,
            @Param("toDate") LocalDate toDate
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


    @Query("""
    select 
        coalesce(sum(p.totalAmount), 0),
        count(p),
        coalesce(avg(p.totalAmount), 0)
    from Payment p
    join Reservation r on p.reservationId = r.id
    where (:designerId is null or r.designerId = :designerId)
      and p.paymentStatus = :status
      and p.createdAt >= :startAt
      and p.createdAt < :endAt
    """)
    List<Object[]> aggregateSalesForPeriodByDesigner(
            @Param("designerId") UUID designerId,
            @Param("status") PaymentStatus status,
            @Param("startAt") java.time.Instant startAt,
            @Param("endAt") java.time.Instant endAt
    );

}
