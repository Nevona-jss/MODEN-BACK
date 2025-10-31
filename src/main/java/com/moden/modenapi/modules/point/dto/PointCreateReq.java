package com.moden.modenapi.modules.point.dto;

import com.moden.modenapi.common.enums.PointType;
import java.math.BigDecimal;
import java.util.UUID;

public record PointCreateReq(
        UUID paymentId,
        PointType type,
        BigDecimal amount
) {}
