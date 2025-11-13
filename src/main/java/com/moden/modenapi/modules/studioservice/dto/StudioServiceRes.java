package com.moden.modenapi.modules.studioservice.dto;

import com.moden.modenapi.common.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Studio service detail (with used products)")
public record StudioServiceRes(

        @Schema(description = "Service ID")
        UUID id,

        @Schema(description = "Studio ID")
        UUID studioId,

        @Schema(description = "Service type")
        ServiceType serviceType,

        @Schema(description = "After service memo")
        String afterService,

        @Schema(description = "Duration (min)")
        int durationMin,

        @Schema(description = "Service price")
        BigDecimal servicePrice,

        @Schema(description = "Designer tip percent")
        BigDecimal designerTipPercent,

        @Schema(description = "Products used in this service")
        List<ServiceUsedProductRes> products,

        @Schema(description = "Created at")
        java.time.Instant createdAt,

        @Schema(description = "Updated at")
        java.time.Instant updatedAt
) {}
