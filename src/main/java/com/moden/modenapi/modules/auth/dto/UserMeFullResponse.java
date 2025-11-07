package com.moden.modenapi.modules.auth.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record UserMeFullResponse(
        UUID userId,
        String fullName,
        String phone,
        String role,

        UUID detailId,
        String detailName,
        Instant createdAt,

        String detailType,   // "STUDIO" | "DESIGNER" | "CUSTOMER" | "NONE"
        Object detail        // StudioRes | DesignerResponse | CustomerResponse | null
) {}
