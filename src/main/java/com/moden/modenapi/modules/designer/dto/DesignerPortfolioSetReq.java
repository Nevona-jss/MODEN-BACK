package com.moden.modenapi.modules.designer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record DesignerPortfolioSetReq(
        @NotEmpty
        @Schema(description = "Tartibni to'liq qayta o'rnatish uchun to‘liq ID ro'yxati (0..n)")
        List<UUID> itemIds
) {}
