package com.moden.modenapi.modules.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Response DTO for studio point policy")
public record StudioPointPolicyRes(
        UUID id,
        UUID studioId,
        BigDecimal pointRate,
        Instant createdAt,
        Instant updatedAt
) {}
