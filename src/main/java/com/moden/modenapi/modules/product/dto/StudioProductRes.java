package com.moden.modenapi.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Product response data")
public record StudioProductRes(
        UUID id,
        UUID studioId,
        String name,
        String category,
        String type,
        BigDecimal price,
        int stock,
        String image,
        Instant createdAt,
        Instant updatedAt
) {}
