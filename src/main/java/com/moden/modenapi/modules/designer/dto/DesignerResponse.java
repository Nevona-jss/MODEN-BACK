package com.moden.modenapi.modules.designer.dto;

import com.moden.modenapi.common.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DesignerResponse(

        // ---------- Identifiers ----------
        @Schema(description = "Designer detail ID")
        UUID id,

        @Schema(description = "Linked user ID")
        UUID userId,

        @Schema(description = "Hair studio ID this designer belongs to")
        UUID studioId,

        @Schema(description = "Designer login code (server-generated, e.g., DS-XXXXX-12345)")
        String idForLogin,

        // ---------- Auth / Role ----------
        @Schema(description = "Effective role (always DESIGNER for designers)")
        Role role,

        // ---------- Contact ----------
        @Schema(description = "Phone number of the designer")
        String phone,

        // ---------- Profile ----------
        @Schema(description = "Short bio/intro")
        String bio,

        // ---------- Portfolio ----------
        @Schema(description = "Ordered portfolio items")
        List<PortfolioItemRes> portfolio,

        // ---------- Timestamps ----------
        @Schema(description = "Creation time (UTC)")
        Instant createdAt,

        @Schema(description = "Last update time (UTC)")
        Instant updatedAt
) {}
