package com.moden.modenapi.modules.reservation.repository;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.reservation.model.Reservation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends BaseRepository<Reservation, UUID> {

    @Query("""
        select r
        from Reservation r
        where (:designerId is null or r.designerId = :designerId)
          and (:customerId is null or r.customerId = :customerId)
          and (:serviceId  is null or r.serviceId  = :serviceId)
          and (:status    is null or r.status     = :status)
          and (:fromDate  is null or r.reservationDate >= :fromDate)
          and (:toDate    is null or r.reservationDate <  :toDate)
        """)
    List<Reservation> searchDynamic(
            @Param("designerId") UUID designerId,
            @Param("customerId") UUID customerId,
            @Param("serviceId")  UUID serviceId,
            @Param("status")     ReservationStatus status,
            @Param("fromDate")   LocalDate fromDate,
            @Param("toDate")     LocalDate toDate,
            Pageable pageable
    );

    // har bir customer uchun oxirgi COMPLETED visit
    @Query("""
        select r.customerId, max(r.reservationDate), max(r.endTime)
        from Reservation r
        where r.customerId in :customerIds
          and r.status = :status
        group by r.customerId
    """)
    List<Object[]> findLastVisitForCustomers(
            @Param("customerIds") List<UUID> customerIds,
            @Param("status") ReservationStatus status
    );

    List<Reservation> findByCustomerId(UUID customerId);

    List<Reservation> findByDesignerId(UUID designerId);

    // ✅ Overlap check (double booking) for designer on same day
    @Query("""
        select case when count(r) > 0 then true else false end
        from Reservation r
        where r.designerId = :designerId
          and r.status = :status
          and r.reservationDate = :reservationDate
          and r.startTime < :endTime
          and r.endTime   > :startTime
        """)
    boolean existsOverlappingForDesigner(
            @Param("designerId") UUID designerId,
            @Param("reservationDate") LocalDate reservationDate,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("status") ReservationStatus status
    );
    @Query("""
    select r
    from Reservation r
    where r.customerId = :customerId
      and r.status = :status
      and r.deletedAt is null
    order by r.reservationDate desc, r.startTime desc
    """)
    List<Reservation> findLatestOneForCustomer(
            @Param("customerId") UUID customerId,
            @Param("status") ReservationStatus status,
            Pageable pageable
    );

}
