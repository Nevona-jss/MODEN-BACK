package com.moden.modenapi.modules.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Active (usable) point summary for customer")
public record PointActiveSummaryRes(
        @Schema(description = "Active points (earned - used)")
        BigDecimal activePoint
) {}
