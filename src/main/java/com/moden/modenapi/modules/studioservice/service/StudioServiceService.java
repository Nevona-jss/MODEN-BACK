package com.moden.modenapi.modules.studioservice.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.studioservice.dto.*;
import com.moden.modenapi.modules.studioservice.model.StudioService;
import com.moden.modenapi.modules.studioservice.repository.StudioServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class StudioServiceService extends BaseService<StudioService> {

    private final StudioServiceRepository studioServiceRepository;

    @Override
    protected StudioServiceRepository getRepository() {
        return studioServiceRepository;
    }

    // ----------------------------------------------------------------------
    // 🔹 CREATE
    // ----------------------------------------------------------------------
    public StudioServiceRes createService(UUID studioId, StudioServiceCreateReq req) {
        StudioService entity = StudioService.builder()
                .studioId(studioId)
                .designerId(req.designerId())
                .customerId(req.customerId())
                .serviceType(req.serviceType())
                .reasonForVisiting(req.reasonForVisiting())
                .reservedDate(req.reservedDate())
                .startAt(req.startAt())
                .endAt(req.endAt())
                .description(req.description())
                .durationMin(req.durationMin())
                .servicePrice(req.servicePrice())
                .build();

        create(entity);
        return mapToRes(entity);
    }

    // ----------------------------------------------------------------------
    // 🔹 UPDATE
    // ----------------------------------------------------------------------
    public StudioServiceRes updateService(UUID serviceId, StudioServiceUpdateReq req) {
        StudioService entity = studioServiceRepository.findActiveById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));

        if (req.serviceType() != null) entity.setServiceType(req.serviceType());
        if (req.reasonForVisiting() != null) entity.setReasonForVisiting(req.reasonForVisiting());
        if (req.reservationStatus() != null) entity.setReservationStatus(req.reservationStatus());
        if (req.reservedDate() != null) entity.setReservedDate(req.reservedDate());
        if (req.startAt() != null) entity.setStartAt(req.startAt());
        if (req.endAt() != null) entity.setEndAt(req.endAt());
        if (req.description() != null) entity.setDescription(req.description());
        if (req.durationMin() != null) entity.setDurationMin(req.durationMin());
        if (req.servicePrice() != null) entity.setServicePrice(req.servicePrice());

        entity.setUpdatedAt(Instant.now());
        update(entity);
        return mapToRes(entity);
    }

    // ----------------------------------------------------------------------
    // 🔹 GET ONE
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public StudioServiceRes getService(UUID serviceId) {
        StudioService entity = studioServiceRepository.findActiveById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        return mapToRes(entity);
    }

    // ----------------------------------------------------------------------
    // 🔹 GET ALL BY STUDIO
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<StudioServiceRes> getAllByStudio(UUID studioId) {
        return studioServiceRepository.findAllActiveByStudioId(studioId)
                .stream()
                .map(this::mapToRes)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------------
    // 🔹 DELETE
    // ----------------------------------------------------------------------
    public void deleteService(UUID serviceId) {
        StudioService entity = studioServiceRepository.findActiveById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        entity.setDeletedAt(Instant.now());
        update(entity);
    }

    // ----------------------------------------------------------------------
    // 🔹 MAPPER
    // ----------------------------------------------------------------------
    private StudioServiceRes mapToRes(StudioService s) {
        return new StudioServiceRes(
                s.getId(),
                s.getStudioId(),
                s.getDesignerId(),
                s.getCustomerId(),
                s.getServiceType(),
                s.getReservationStatus(),
                s.getReasonForVisiting(),
                s.getReservedDate(),
                s.getStartAt(),
                s.getEndAt(),
                s.getDescription(),
                s.getDurationMin(),
                s.getServicePrice(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }


// 🔹 GET DAILY
// ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<StudioServiceRes> getDailyServices(UUID studioId, LocalDate date) {
        List<StudioService> list = studioServiceRepository.findAllByStudioIdAndDate(studioId, date);
        return list.stream().map(this::mapToRes).collect(Collectors.toList());
    }

    // ----------------------------------------------------------------------
// 🔹 GET WEEKLY
// ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<StudioServiceRes> getWeeklyServices(UUID studioId, LocalDate dateInWeek) {
        LocalDate startOfWeek = dateInWeek.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = dateInWeek.with(DayOfWeek.SUNDAY);
        List<StudioService> list = studioServiceRepository.findAllByStudioIdAndDateRange(studioId, startOfWeek, endOfWeek);
        return list.stream().map(this::mapToRes).collect(Collectors.toList());
    }

    // ----------------------------------------------------------------------
// 🔹 GET MONTHLY
// ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<StudioServiceRes> getMonthlyServices(UUID studioId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.with(TemporalAdjusters.lastDayOfMonth());
        List<StudioService> list = studioServiceRepository.findAllByStudioIdAndDateRange(studioId, start, end);
        return list.stream().map(this::mapToRes).collect(Collectors.toList());
    }

    // ----------------------------------------------------------------------
// 🔹 GET BETWEEN DATES
// ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<StudioServiceRes> getServicesBetween(UUID studioId, LocalDate startDate, LocalDate endDate) {
        List<StudioService> list = studioServiceRepository.findAllByStudioIdAndDateRange(studioId, startDate, endDate);
        return list.stream().map(this::mapToRes).collect(Collectors.toList());
    }


}
