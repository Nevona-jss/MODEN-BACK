package com.moden.modenapi.modules.designer.dto;

import java.util.UUID;

public record PortfolioItemRes(
        UUID id,
        String imageUrl,   // public URL: /uploads/<...>
        String caption
) {}