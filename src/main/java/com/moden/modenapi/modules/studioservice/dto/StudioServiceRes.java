package com.moden.modenapi.modules.studioservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Studio service detail")
public record StudioServiceRes(

        @Schema(description = "Service ID")
        UUID id,

        @Schema(description = "Service name")
        String serviceName,

        @Schema(description = "After service memo")
        String afterService,

        @Schema(description = "Duration (min)")
        int durationMin,

        @Schema(description = "Service price")
        BigDecimal servicePrice,

        @Schema(description = "Designer tip percent")
        BigDecimal designerTipPercent,

        @Schema(description = "Created at")
        Instant createdAt,

        @Schema(description = "Updated at")
        Instant updatedAt
) {}
