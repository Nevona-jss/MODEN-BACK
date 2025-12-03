package com.moden.modenapi.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;


@Schema(description = "Product creation request (Studio adds a product)")
public record StudioProductCreateReq(
        String productName,
        BigDecimal price,
        String notes,
        BigDecimal volumeLiters,
        BigDecimal designerTipPercent
) {}
