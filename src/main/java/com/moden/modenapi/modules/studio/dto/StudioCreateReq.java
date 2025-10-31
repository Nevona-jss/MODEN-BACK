// com.moden.modenapi.modules.studio.dto.StudioCreateReq.java
package com.moden.modenapi.modules.studio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

@Schema(name = "StudioCreateReq", description = "Create a Hair Studio (and studio login credential).")
public record StudioCreateReq(

        @Schema(description = "Business registration number", example = "123-45-67890", required = true)
        @NotBlank @Size(max = 100)
        String businessNo,

        @Schema(description = "Owner fullName", example = "Alice Kim", required = true)
        @NotBlank @Size(max = 100)
        String ownerName,

        @Schema(description = "Custom studio login ID (optional)", example = "ST-MODEN-12345")
        String idForLogin,

        @Schema(description = "Login password (plain). Will be hashed before saving.", example = "moden1234!", required = true)
        @NotBlank @Size(min = 6, max = 100)
        String password,

        @NotBlank
        String fullName,

        // images (optional)
        @Schema(description = "Main logo image URL")
        @Size(max = 500)
        String logoImageUrl,

        @Schema(description = "Banner image URL for studio header")
        @Size(max = 500)
        String bannerImageUrl,

        @Schema(description = "Profile image URL (small avatar)")
        @Size(max = 255)
        String profileImageUrl,

        // geo (optional)
        @Schema(description = "Latitude", example = "37.5665")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0",  message = "Latitude must be <= 90")
        BigDecimal latitude,

        @Schema(description = "Longitude", example = "126.9780")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0",  message = "Longitude must be <= 180")
        BigDecimal longitude
) {}
