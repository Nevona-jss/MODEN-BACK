package com.moden.modenapi.modules.studioservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Product information used in a service request")
public record ServiceUsedProductReq(

        @NotNull
        @Schema(description = "Product ID (UUID of studio_product)", example = "1bde8c8b-1c52-4e0c-b93a-f1d38b6a1a80")
        UUID productId,

        @Positive
        @Schema(description = "Quantity of product used", example = "1")
        int quantity,

        @Positive
        @Schema(description = "Unit price of product", example = "15000.00")
        BigDecimal price
) {}
