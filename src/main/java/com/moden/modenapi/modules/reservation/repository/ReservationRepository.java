package com.moden.modenapi.modules.reservation.repository;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.reservation.model.Reservation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends BaseRepository<Reservation, UUID> {


    @Query("""
    select r
    from Reservation r
    where (:designerId is null or r.designerId = :designerId)
      and (:customerId is null or r.customerId = :customerId)
      and (:serviceId is null or r.serviceId = :serviceId)
      and (:status    is null or r.status = :status)
      and (:fromDt    is null or r.reservationAt >= :fromDt)
      and (:toDt      is null or r.reservationAt <  :toDt)
""")
    List<Reservation> searchDynamic(
            @Param("designerId") UUID designerId,
            @Param("customerId") UUID customerId,
            @Param("serviceId") UUID serviceId,
            @Param("status") ReservationStatus status,
            @Param("fromDt") LocalDateTime fromDt,
            @Param("toDt") LocalDateTime toDt,
            Pageable pageable
    );



    // ✅ mijoz bo‘yicha hamma rezervatsiyalar
    List<Reservation> findByCustomerId(UUID customerId);

    // ✅ dizayner bo‘yicha hamma rezervatsiyalar
    List<Reservation> findByDesignerId(UUID designerId);


    // ✅ double booking check
    boolean existsByDesignerIdAndReservationAtAndStatus(
            UUID designerId,
            LocalDateTime reservationAt,
            ReservationStatus status
    );
}
