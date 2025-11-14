package com.moden.modenapi.modules.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Studio point policy response")
public record StudioPointPolicyRes(
        UUID id,
        UUID studioId,
        BigDecimal pointRate,   // 5.00 → 5%

        Instant createdAt,
        Instant updatedAt
) {}
