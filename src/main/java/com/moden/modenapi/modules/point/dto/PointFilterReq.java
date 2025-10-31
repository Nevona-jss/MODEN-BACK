package com.moden.modenapi.modules.point.dto;

import com.moden.modenapi.common.enums.PointType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Point filter request")
public record PointFilterReq(
        @Schema(description = "User ID (required)")
        UUID userId,

        @Schema(description = "Transaction type (EARNED or USED)")
        PointType type,

        @Schema(description = "Date range filter: today / week / month")
        String dateRange
) {}
