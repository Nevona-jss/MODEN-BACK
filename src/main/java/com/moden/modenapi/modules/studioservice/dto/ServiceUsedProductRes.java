package com.moden.modenapi.modules.studioservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Product info used in a service (response)")
public record ServiceUsedProductRes(
        UUID productId,
        int quantity,
        BigDecimal price,
        BigDecimal totalPrice
) {}
