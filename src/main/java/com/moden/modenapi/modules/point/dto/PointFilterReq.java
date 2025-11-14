package com.moden.modenapi.modules.point.dto;

import com.moden.modenapi.common.enums.PointType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Point filter request (used as query params)")
public record PointFilterReq(

        @Schema(description = "Target userId", example = "uuid")
        UUID userId,

        @Schema(description = "Point type (EARN/USE)", example = "EARN")
        PointType type,

        @Schema(description = "From (ISO-8601)", example = "2025-01-01T00:00:00Z")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant from,

        @Schema(description = "To (ISO-8601)", example = "2025-12-31T23:59:59Z")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant to
) {}
