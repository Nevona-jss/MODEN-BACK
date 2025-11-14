package com.moden.modenapi.modules.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Manual point grant request (studio → customer)")
public record PointManualGrantReq(

        @Schema(description = "Target customer userId", example = "uuid")
        UUID userId,

        @Schema(description = "Title / reason", example = "Service recovery bonus")
        String title,

        @Schema(description = "Point amount", example = "5000.00")
        BigDecimal amount
) {}
