package com.moden.modenapi.modules.booking.service;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.booking.dto.*;
import com.moden.modenapi.modules.booking.model.Reservation;
import com.moden.modenapi.modules.booking.repository.ReservationRepository;
import com.moden.modenapi.modules.serviceitem.repository.StudioServiceItemRepository;
import com.moden.modenapi.modules.studio.repository.HairStudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ReservationService extends BaseService<Reservation, UUID> {

    private final ReservationRepository resRepo;
    private final UserRepository userRepo;
    private final StudioServiceItemRepository srvRepo;
    private final HairStudioRepository studioRepo;

    @Override
    protected JpaRepository<Reservation, UUID> getRepository() {
        return resRepo;
    }

    public ReservationRes createReservation(ReservationCreateReq req) {
        var customer = userRepo.findById(req.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        var designer = userRepo.findById(req.designerId())
                .orElseThrow(() -> new IllegalArgumentException("Designer not found"));
        var service = srvRepo.findById(req.serviceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
        var studio = studioRepo.findById(req.studioId())
                .orElseThrow(() -> new IllegalArgumentException("Studio not found"));

        var reservation = Reservation.builder()
                .customer(customer)
                .designer(designer)
                .service(service)
                .studio(studio)
                .reservedAt(req.reservedAt() != null ? req.reservedAt() : Instant.now())
                .status(ReservationStatus.PENDING)
                .externalRef(req.externalRef())
                .build();

        reservation = save(reservation);

        return new ReservationRes(
                reservation.getId(),
                customer.getId(),
                designer.getId(),
                service.getId(),
                studio.getId(),
                reservation.getStatus(),
                reservation.getReservedAt(),
                reservation.getExternalRef()
        );
    }

    @Transactional(readOnly = true)
    public List<ReservationRes> getAllReservations() {
        return getAll().stream()
                .map(r -> new ReservationRes(
                        r.getId(),
                        r.getCustomer().getId(),
                        r.getDesigner().getId(),
                        r.getService().getId(),
                        r.getStudio().getId(),
                        r.getStatus(),
                        r.getReservedAt(),
                        r.getExternalRef()
                ))
                .toList();
    }
}
