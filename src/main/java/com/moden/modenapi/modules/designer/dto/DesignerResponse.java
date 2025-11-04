package com.moden.modenapi.modules.designer.dto;

import com.moden.modenapi.common.enums.Role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DesignerResponse(
        UUID id,
        UUID userId,
        UUID studioId,
        String idForLogin,
        String phone,
        Role role,
        String bio,
        List<PortfolioItemRes> portfolio,
        Instant createdAt,
        Instant updatedAt
) {}
