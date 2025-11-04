package com.moden.modenapi.modules.designer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record DesignerPortfolioRemoveReq(
        @NotEmpty
        @Schema(description = "O'chiriladigan portfolio item ID lar")
        List<UUID> itemIds
) {}
