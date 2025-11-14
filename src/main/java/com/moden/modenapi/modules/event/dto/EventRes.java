package com.moden.modenapi.modules.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Event response data")
public record EventRes(
        UUID id,
        UUID studioId,
        String title,
        String description,
        String imageUrl,
        BigDecimal discount,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt,
        Instant updatedAt
) {}
