package com.moden.modenapi.modules.studioservice.dto;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Request DTO for creating a studio service reservation")
public record StudioServiceCreateReq(

        @NotNull
        @Schema(description = "Designer ID", example = "6a0dc9da-0284-4e9d-89ed-5b7b6a77b3c5")
        UUID designerId,

        @NotNull
        @Schema(description = "Customer ID", example = "8d1a7c3f-1b9b-4202-97d2-61f9b364f841")
        UUID customerId,

        @NotNull
        @Schema(description = "Service type", example = "CUT")
        ServiceType serviceType,

        @Schema(description = "Reason for visiting", example = "새로운 염색 스타일 시도")
        String reasonForVisiting,

        @NotNull
        @Schema(description = "Reservation date", example = "2025-10-20")
        LocalDate reservedDate,

        @NotNull
        @Schema(description = "Start time", example = "2025-10-20T18:00:00")
        LocalDateTime startAt,

        @NotNull
        @Schema(description = "End time", example = "2025-10-20T19:00:00")
        LocalDateTime endAt,

        @Schema(description = "Service description", example = "새로운 봄 트렌드 컬러와 함께 커트 포함 패키지")
        String description,

        @Positive
        @Schema(description = "Duration in minutes", example = "60")
        int durationMin,

        @Positive
        @Schema(description = "Base service price", example = "45000.00")
        BigDecimal servicePrice
) {}
