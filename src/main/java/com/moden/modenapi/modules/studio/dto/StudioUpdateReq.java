// com.moden.modenapi.modules.studio.dto.StudioUpdateReq.java
package com.moden.modenapi.modules.studio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(name = "StudioUpdateReq", description = "Partial update for Hair Studio (all fields optional).")
public record StudioUpdateReq(

        // ⚠️ Studio/self update: IGNORE (read-only for studio)
        @Schema(description = "Owner full name (User.fullName) — read-only for studio")
        @Size(max = 100)
        String fullName,

        // ⚠️ Studio/self update: IGNORE (read-only for studio)
        @Schema(description = "Business registration number — read-only for studio")
        @Size(max = 50)
        String businessNo,

        @Size(min = 1, max = 100)
        String ownerName,

        @Schema(description = "Studio phone number", example = "02-123-4567")
        @Size(max = 50)
        String studioPhone,

        @Schema(description = "Studio owner phone number", example = "010-5452-1223-4567")
        @Size(max = 50)
        String phone,

        @Schema(description = "Studio address", example = "Seoul, Gangnam-gu, Teheran-ro 123")
        @Size(max = 255)
        String address,

        @Schema(description = "Studio description / introduction")
        String description,

        // images (URL)
        @Schema(description = "Main logo image URL")
        @Size(max = 500)
        String logoImageUrl,

        @Schema(description = "Banner image URL for studio header")
        @Size(max = 500)
        String bannerImageUrl,

        // socials
        @Schema(description = "Naver URL", example = "https://naver.me/modenhair")
        @Size(max = 255)
        @Pattern(regexp = "^(https?://)?(www\\.)?naver\\.(com|me)/.*$", message = "Invalid Naver URL format")
        String naver,

        @Schema(description = "Kakao URL", example = "https://pf.kakao.com/_moden")
        @Size(max = 255)
        @Pattern(regexp = "^(https?://)?(pf\\.kakao\\.com|open\\.kakao\\.com)/.*$", message = "Invalid Kakao URL format")
        String kakao,


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
