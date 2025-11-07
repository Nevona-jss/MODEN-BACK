package com.moden.modenapi.modules.auth.dto;

import java.util.UUID;

public record LoginResStudioAndDesigner(
        String accessToken,
        String refreshToken,
        String role,        // DESIGNER | HAIR_STUDIO
        UUID userId,
        UUID entityId,      // designerId yoki studioId
        String idForLogin
) {}
