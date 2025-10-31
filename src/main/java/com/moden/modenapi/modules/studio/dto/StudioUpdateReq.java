// com.moden.modenapi.modules.studio.dto.StudioUpdateReq.java
package com.moden.modenapi.modules.studio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

@Schema(name = "StudioUpdateReq", description = "Partial update for Hair Studio (all fields optional).")
public record StudioUpdateReq(
        @Schema(description = "Studio phone number", example = "02-123-4567")
        @Size(max = 50)
        String studioPhone,

        @Schema(description = "Studio address", example = "Seoul, Gangnam-gu, Teheran-ro 123")
        @Size(max = 255)
        String address,

        @Schema(description = "Owner phone number (not persisted yet)", example = "010-9999-8888")
        @Size(max = 50)
        String ownerPhone,

        @Schema(description = "Studio description / introduction")
        String description,

        // images
        @Schema(description = "Main logo image URL")
        @Size(max = 500)
        String logoImageUrl,

        @Schema(description = "Banner image URL for studio header")
        @Size(max = 500)
        String bannerImageUrl,

        @Schema(description = "Profile image URL (small avatar)")
        @Size(max = 255)
        String profileImageUrl,

        // socials
        @Schema(description = "Instagram URL", example = "https://instagram.com/moden_hair")
        @Size(max = 255)
        @Pattern(regexp = "^(https?://)?(www\\.)?instagram\\.com/.*$", message = "Invalid Instagram URL format")
        String instagram,

        @Schema(description = "Naver URL", example = "https://naver.me/modenhair")
        @Size(max = 255)
        @Pattern(regexp = "^(https?://)?(www\\.)?naver\\.(com|me)/.*$", message = "Invalid Naver URL format")
        String naver,

        @Schema(description = "Kakao URL", example = "https://pf.kakao.com/_moden")
        @Size(max = 255)
        @Pattern(regexp = "^(https?://)?(pf\\.kakao\\.com|open\\.kakao\\.com)/.*$", message = "Invalid Kakao URL format")
        String kakao,

        @Schema(description = "Parking information", example = "Free parking available in basement")
        @Size(max = 200)
        String parkingInfo,

        // geo
        @Schema(description = "Latitude", example = "37.5665")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0",  message = "Latitude must be <= 90")
        BigDecimal latitude,

        @Schema(description = "Longitude", example = "126.9780")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0",  message = "Longitude must be <= 180")
        BigDecimal longitude
) {}
