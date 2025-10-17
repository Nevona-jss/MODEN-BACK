package com.moden.modenapi.modules.studio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a new hair studio.
 */
@Schema(name = "StudioCreateReq", description = "Request for creating a hair studio. Only the first 3 fields are required.")
public record StudioCreateReq(

        @Schema(description = "Studio name", example = "Moden Hair", required = true)
        @NotBlank(message = "Studio name is required")
        @Size(max = 150, message = "Studio name must be under 150 characters")
        String name,

        @Schema(description = "Business registration number", example = "123-45-67890", required = true)
        @NotBlank(message = "Business number is required")
        @Size(max = 100, message = "Business number must be under 100 characters")
        String businessNo,

        @Schema(description = "Owner name", example = "Alice Kim", required = true)
        @NotBlank(message = "Owner name is required")
        @Size(max = 100, message = "Owner name must be under 100 characters")
        String owner,

        // optional fields
        @Schema(description = "Owner phone number", example = "+998901234567", required = false)
        @Size(max = 50, message = "Owner phone number must be under 50 characters")
        String ownerPhone,

        @Schema(description = "Studio phone number", example = "+998901112233", required = false)
        @Size(max = 50, message = "Studio phone number must be under 50 characters")
        String studioPhone,

        @Schema(description = "Address", example = "Tashkent, Street 1", required = false)
        @Size(max = 255, message = "Address must be under 255 characters")
        String address,

        @Schema(description = "Logo URL", example = "https://cdn.example.com/logo.png", required = false)
        @Size(max = 255, message = "Logo URL must be under 255 characters")
        String logo,

        @Schema(description = "Instagram profile URL", example = "https://instagram.com/modenhair", required = false)
        @Size(max = 255, message = "Instagram link must be under 255 characters")
        @Pattern(
                regexp = "^(https?://)?(www\\.)?instagram\\.com/.*$",
                message = "Invalid Instagram URL format"
        )
        String instagram,

        @Schema(description = "Naver link", example = "https://www.naver.com/..", required = false)
        @Size(max = 255, message = "Naver link must be under 255 characters")
        @Pattern(
                regexp = "^(https?://)?(www\\.)?naver\\.com/.*$",
                message = "Invalid Naver URL format"
        )
        String naver
) {}
