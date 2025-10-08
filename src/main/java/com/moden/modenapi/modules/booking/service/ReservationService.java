package com.moden.modenapi.modules.booking.service;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.booking.dto.*;
import com.moden.modenapi.modules.booking.model.Reservation;
import com.moden.modenapi.modules.booking.repository.ReservationRepository;
import com.moden.modenapi.modules.serviceitem.repository.StudioServiceItemRepository;
import com.moden.modenapi.modules.studio.repository.HairStudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository resRepo;
    private final UserRepository userRepo;
    private final StudioServiceItemRepository srvRepo;
    private final HairStudioRepository studioRepo;

    public ReservationRes create(ReservationCreateReq req){
        var customer = userRepo.findById(req.customerId()).orElseThrow();
        var designer = userRepo.findById(req.designerId()).orElseThrow();
        var service  = srvRepo.findById(req.serviceId()).orElseThrow();
        var studio   = studioRepo.findById(req.studioId()).orElseThrow();

        var r = Reservation.builder()
                .customer(customer).designer(designer).service(service).studio(studio)
                .reservedAt(req.reservedAt() != null ? req.reservedAt() : Instant.now())
                .status(ReservationStatus.PENDING).externalRef(req.externalRef())
                .build();
        r = resRepo.save(r);

        return new ReservationRes(
                r.getId(), customer.getId(), designer.getId(), service.getId(), studio.getId(),
                r.getStatus(), r.getReservedAt(), r.getExternalRef()
        );
    }

    @Transactional(readOnly = true)
    public List<ReservationRes> list(){
        return resRepo.findAll().stream().map(r ->
                new ReservationRes(
                        r.getId(), r.getCustomer().getId(), r.getDesigner().getId(),
                        r.getService().getId(), r.getStudio().getId(),
                        r.getStatus(), r.getReservedAt(), r.getExternalRef()
                )
        ).toList();
    }
}
