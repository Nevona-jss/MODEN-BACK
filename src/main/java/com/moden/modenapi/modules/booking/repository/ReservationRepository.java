package com.moden.modenapi.modules.booking.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.moden.modenapi.modules.booking.model.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    List<Reservation> findAllByDesigner_Id(UUID designerId);
}
