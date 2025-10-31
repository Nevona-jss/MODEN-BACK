package com.moden.modenapi.modules.studioservice.dto;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Response DTO for studio service reservation details")
public record StudioServiceRes(
        UUID id,
        UUID studioId,
        UUID designerId,
        UUID customerId,
        ServiceType serviceType,
        ReservationStatus reservationStatus,
        String reasonForVisiting,
        LocalDate reservedDate,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String description,
        int durationMin,
        BigDecimal servicePrice,
        Instant createdAt,
        Instant updatedAt
) {}
