package com.moden.modenapi.modules.studioservice.dto;

import com.moden.modenapi.common.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Request DTO for creating a studio service")
public record StudioServiceCreateRequest(

        @NotNull
        @Schema(
                description = "Service type",
                example = "CUT",
                allowableValues = {"PERM", "CARE", "CUT", "COLOR"}
        )
        ServiceType serviceType,

        @Schema(description = "After service memo / 안내 문구")
        String afterService,

        @Positive
        @Schema(description = "Service duration in minutes", example = "60")
        int durationMin,

        @NotNull
        @Schema(description = "Base service price", example = "35000.00")
        BigDecimal servicePrice,

        @Schema(description = "Designer tip percent", example = "10.00")
        BigDecimal designerTipPercent,

        @Schema(description = "Products used in this service")
        List<ServiceUsedProductReq> products
) {}
