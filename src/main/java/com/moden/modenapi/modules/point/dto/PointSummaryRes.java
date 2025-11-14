package com.moden.modenapi.modules.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Customer point summary")
public record PointSummaryRes(
        @Schema(description = "Total earned points")
        BigDecimal totalEarned,
        @Schema(description = "Total used points")
        BigDecimal totalUsed,
        @Schema(description = "Current balance (earned - used)")
        BigDecimal balance
) {}
