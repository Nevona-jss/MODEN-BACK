package com.moden.modenapi.modules.point.dto;

import com.moden.modenapi.common.enums.PointType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Point record response")
public record PointRes(
        UUID id,
        UUID userId,
        UUID paymentId,
        String title,
        PointType type,
        BigDecimal amount,
        Instant createdAt
) {}
