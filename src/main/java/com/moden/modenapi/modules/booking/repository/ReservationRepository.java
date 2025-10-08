package com.moden.modenapi.modules.booking.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.moden.modenapi.modules.booking.model.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {}
