package com.moden.modenapi.modules.point.dto;

public record PromoDto(
        Long id,
        String title,
        String description,
        String imageUrl,
        boolean active
) {}
