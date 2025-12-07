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
     *  - studioId  : Reservation.studioId
     *  - designerId: Reservation.designerId
     *  - status    : Payment.paymentStatus
     *  - from/to   : Reservation.reservationDate
     */
    @Query("""
        select p
        from Payment p
        join Reservation r on p.reservationId = r.id
        where (:studioId   is null or r.studioId      = :studioId)
          and (:designerId is null or r.designerId    = :designerId)
          and (:status     is null or p.paymentStatus = :status)
          and (:fromDate   is null or r.reservationDate >= :fromDate)
          and (:toDate     is null or r.reservationDate <= :toDate)
        """)
    List<Payment> searchPayments(
            @Param("studioId")   UUID studioId,
            @Param("designerId") UUID designerId,
            @Param("status")     PaymentStatus status,
            @Param("fromDate")   LocalDate fromDate,
            @Param("toDate")     LocalDate toDate
    );

    Optional<Payment> findByReservationId(UUID reservationId);

    /**
     * 오늘 매출 통계용 (스튜디오/디자이너 공통)
     * userId 가 스튜디오 ID 이면 studioId 기준,
     * 디자이너 ID 이면 designerId 기준으로 집계.
     */
    @Query("""
        select 
            coalesce(sum(p.totalAmount), 0),
            count(p),
            coalesce(avg(p.totalAmount), 0)
        from Payment p
        join Reservation r on p.reservationId = r.id
        where (:userId is null
               or r.studioId   = :userId
               or r.designerId = :userId
        )
          and p.paymentStatus = :status
          and p.createdAt >= :startAt
          and p.createdAt < :endAt
        """)
    List<Object[]> aggregateSalesForPeriod(
            @Param("userId")  UUID userId,
            @Param("status")  PaymentStatus status,
            @Param("startAt") java.time.Instant startAt,
            @Param("endAt")   java.time.Instant endAt
    );

    // 필요하면 유지, 안 쓰면 삭제해도 됨
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
            @Param("endAt")   java.time.Instant endAt
    );


}
