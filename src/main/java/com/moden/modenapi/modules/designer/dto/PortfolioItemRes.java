package com.moden.modenapi.modules.designer.dto;

import com.moden.modenapi.common.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Single portfolio item response")
public record PortfolioItemRes(
        UUID id,
        String imageUrl,
        String caption
) {}
