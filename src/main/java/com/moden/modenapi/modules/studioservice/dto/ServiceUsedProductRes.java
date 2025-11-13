package com.moden.modenapi.modules.studioservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Product info used in a service (response)")
public record ServiceUsedProductRes(

        @Schema(description = "Product ID")
        UUID productId,

        @Schema(description = "Product name")
        String productName,

        @Schema(description = "Quantity")
        int quantity,

        @Schema(description = "Unit price")
        BigDecimal price,

        @Schema(description = "Total price (quantity × price)")
        BigDecimal totalPrice
) {}
