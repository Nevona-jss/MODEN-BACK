package com.moden.modenapi.modules.studioservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Request DTO for creating a studio service")
public record StudioServiceCreateRequest(

        @NotBlank
        @Schema(
                description = "Service name (예: 여성 컷, 남성 펌 등)",
                example = "여성 컷"
        )
        String serviceName,

        @Schema(description = "After service memo / 안내 문구")
        String afterService,

        @Positive
        @Schema(description = "Service duration in minutes", example = "60")
        int durationMin,

        @NotNull
        @Schema(description = "Base service price", example = "35000.00")
        BigDecimal servicePrice,

        @Schema(description = "Designer tip percent", example = "10.00")
        BigDecimal designerTipPercent
) {}
