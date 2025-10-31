package com.moden.modenapi.modules.studioservice.dto;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Request DTO for updating a studio service reservation")
public record StudioServiceUpdateReq(

        @Schema(description = "Service type", example = "PERM")
        ServiceType serviceType,

        @Schema(description = "Reason for visiting", example = "트리트먼트 추가")
        String reasonForVisiting,

        @Schema(description = "Reservation status", example = "COMPLETED")
        ReservationStatus reservationStatus,

        @Schema(description = "Updated reserved date")
        LocalDate reservedDate,

        @Schema(description = "Updated start time")
        LocalDateTime startAt,

        @Schema(description = "Updated end time")
        LocalDateTime endAt,

        @Schema(description = "Updated description")
        String description,

        @Positive
        @Schema(description = "Updated duration", example = "90")
        Integer durationMin,

        @Positive
        @Schema(description = "Updated price", example = "55000.00")
        BigDecimal servicePrice
) {}
