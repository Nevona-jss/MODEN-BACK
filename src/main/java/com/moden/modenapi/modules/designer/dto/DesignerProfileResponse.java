package com.moden.modenapi.modules.designer.dto;

import java.util.UUID;

public record DesignerProfileResponse(
        UUID designerId,
        String bio,
        String portfolioUrl,
        String phonePublic,
        UUID salonId,
        String salonName
) {}
