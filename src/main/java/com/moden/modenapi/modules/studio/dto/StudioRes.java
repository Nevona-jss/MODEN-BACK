// com.moden.modenapi.modules.studio.dto.StudioRes.java
package com.moden.modenapi.modules.studio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "StudioRes", description = "Hair Studio response (safe fields).")
public record StudioRes(
        UUID id,
        String idForLogin,
        String businessNo,
        String ownerName,
        String studioPhone,
        String ownerPhone,      // not stored yet
        String address,
        String description,

        // images
        String profileImageUrl,
        String logoImageUrl,
        String bannerImageUrl,

        // socials
        String instagramUrl,
        String naverUrl,
        String kakaoUrl,

        String parkingInfo,
        BigDecimal latitude,
        BigDecimal longitude
) {}
