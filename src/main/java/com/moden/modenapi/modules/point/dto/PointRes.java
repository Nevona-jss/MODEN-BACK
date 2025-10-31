package com.moden.modenapi.modules.point.dto;

import com.moden.modenapi.common.enums.PointType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PointRes(
        UUID id,
        UUID paymentId,
        PointType type,
        BigDecimal amount,
        Instant createdAt,
        Instant updatedAt
) {}
