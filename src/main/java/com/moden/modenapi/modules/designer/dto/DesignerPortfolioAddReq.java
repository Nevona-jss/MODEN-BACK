package com.moden.modenapi.modules.designer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record DesignerPortfolioAddReq(
        @NotEmpty
        @Schema(description = "Qo'shiladigan portfolio item ID lar")
        List<UUID> itemIds
) {}
