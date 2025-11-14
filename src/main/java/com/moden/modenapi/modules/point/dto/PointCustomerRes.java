package com.moden.modenapi.modules.point.dto;

import com.moden.modenapi.common.enums.PointType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Customer-facing point item")
public record PointCustomerRes(

        @Schema(description = "Point ID")
        UUID id,

        @Schema(description = "Title (e.g. Studio Point, Payment Point)")
        String title,

        @Schema(description = "EARN (berilgan) yoki USE (ishlatilgan)")
        PointType type,

        @Schema(description = "Point amount")
        BigDecimal amount,

        @Schema(description = "Created time (transaction time)")
        Instant createdAt,

        @Schema(description = "Service name (agar payment bilan bog'liq bo'lsa)")
        String serviceName
) {}
