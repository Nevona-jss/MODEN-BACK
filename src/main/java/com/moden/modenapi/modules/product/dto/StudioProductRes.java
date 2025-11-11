package com.moden.modenapi.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;


@Schema(description = "Product response data")
public record StudioProductRes(
        java.util.UUID id,
        java.util.UUID studioId,
        String productName,
        BigDecimal price,
        String notes,
        BigDecimal volumeLiters,
        BigDecimal designerTipPercent,
        java.time.Instant createdAt,
        java.time.Instant updatedAt
) {}
