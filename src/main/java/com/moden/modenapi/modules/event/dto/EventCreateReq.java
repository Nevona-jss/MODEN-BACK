package com.moden.modenapi.modules.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Event creation request (Salon creates an event)")
public record EventCreateReq(

        @NotBlank
        @Schema(description = "Event title", example = "Summer Discount 30%")
        String title,

        @Schema(description = "Event description", example = "30% off on all coloring services this summer.")
        String description,

        @Schema(description = "Event banner image URL", example = "https://cdn.moden.com/events/summer-2025.jpg")
        String imageUrl,

        @Schema(description = "Discount amount (money or percent, depending on business rule)", example = "30000.00")
        BigDecimal discount,

        @NotNull
        @Schema(description = "Start date", example = "2025-07-01")
        LocalDate startDate,

        @NotNull
        @Schema(description = "End date", example = "2025-07-31")
        LocalDate endDate
) {}
