package com.moden.modenapi.modules.reservation.repository;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.reservation.model.Reservation;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends BaseRepository<Reservation, UUID> {

    // 🔹 dizayner + status
    List<Reservation> findByDesignerIdAndStatus(
            UUID designerId,
            ReservationStatus status);

    // ✅ Vaqt oralig‘i bo‘yicha barcha reservationlar
    List<Reservation> findByReservationAtBetween(
            LocalDateTime startAt,
            LocalDateTime endAt
    );
    // ✅ mijoz bo‘yicha hamma rezervatsiyalar
    List<Reservation> findByCustomerId(UUID customerId);

    // ✅ dizayner bo‘yicha hamma rezervatsiyalar
    List<Reservation> findByDesignerId(UUID designerId);

    // ✅ status (RESERVED / CANCELED) bo‘yicha
    List<Reservation> findByStatus(ReservationStatus status);

    // ✅ ma’lum dizayner uchun ma’lum vaqt oralig‘idagi rezervatsiyalar
    List<Reservation> findByDesignerIdAndReservationAtBetween(
            UUID designerId,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    // ✅ ma’lum mijoz uchun ma’lum vaqt oralig‘idagi rezervatsiyalar
    List<Reservation> findByCustomerIdAndReservationAtBetween(
            UUID customerId,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    // ✅ double booking check
    boolean existsByDesignerIdAndReservationAtAndStatus(
            UUID designerId,
            LocalDateTime reservationAt,
            ReservationStatus status
    );

    // 🔹 customer + status (filter uchun)
    List<Reservation> findByCustomerIdAndStatus(
            UUID customerId,
            ReservationStatus status
    );
}
