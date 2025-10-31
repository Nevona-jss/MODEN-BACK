package com.moden.modenapi.modules.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Event update request (partial)")
public record EventUpdateReq(
        String title,
        String description,
        String type,
        String imageUrl,
        LocalDate startDate,
        LocalDate endDate
) {}
