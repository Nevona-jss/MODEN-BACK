package com.moden.modenapi.modules.studio.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "StudioRes", description = "Hair Studio response DTO")
public record StudioRes(

        // ===== Required =====
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        UUID studioId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        UUID ownerUserId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Studio display name")
        String fullName,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Owner phone number")
        String ownerPhone,

        // ===== Optional =====
        String idForLogin,
        String businessNo,
        String ownerName,
        String studioPhone,
        String address,
        String description,
        String profileImageUrl,
        String logoImageUrl,
        String bannerImageUrl,
        String instagramUrl,
        String naverUrl,
        String kakaoUrl,
        String parkingInfo,
        BigDecimal latitude,
        BigDecimal longitude
) {}
