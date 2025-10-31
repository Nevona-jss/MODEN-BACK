package com.moden.modenapi.modules.designer.dto;

import com.moden.modenapi.common.enums.Position;
import com.moden.modenapi.common.enums.Role;

import java.util.UUID;

public record DesignerResponse(
        UUID designerId,
        UUID userId,
        UUID studioId,
        String idForLogin,
        String bio,
        String portfolioUrl,
        Role role,
        Position position
) {}
