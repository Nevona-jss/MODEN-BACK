package com.moden.modenapi.modules.designer.dto;


import java.util.UUID;

public record DesignerDto(
        UUID id,
        String bio,
        String portfolioUrl,
        UUID salonId,
        UUID userId
) {}