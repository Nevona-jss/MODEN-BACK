package com.moden.modenapi.modules.studio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * ✅ StudioCreateReq
 * DTO for creating a new hair studio.
 *
 * Required:
 *  - name
 *  - businessNo
 *  - owner
 *  - ownerPhone
 *
 * Optional:
 *  - studioPhone
 *  - address
 *  - logo
 *  - instagram
 *  - naver
 */
public record StudioCreateReq(

        // 🔹 Required Fields
        @NotBlank(message = "Studio name is required")
        @Size(max = 150, message = "Studio name must be under 150 characters")
        String name,

        @NotBlank(message = "Business number is required")
        @Size(max = 100, message = "Business number must be under 100 characters")
        String businessNo,

        @NotBlank(message = "Owner name is required")
        @Size(max = 100, message = "Owner name must be under 100 characters")
        String owner,

        // 🔹 Optional Fields
        @Size(max = 50, message = "Owner phone number must be under 50 characters")
        String ownerPhone,

        @Size(max = 50, message = "Studio phone number must be under 50 characters")
        String studioPhone,

        @Size(max = 255, message = "Address must be under 255 characters")
        String address,

        @Size(max = 255, message = "Logo URL must be under 255 characters")
        String logo,

        @Size(max = 255, message = "Instagram link must be under 255 characters")
        @Pattern(
                regexp = "^(https?://)?(www\\.)?instagram\\.com/.*$",
                message = "Invalid Instagram URL format"
        )
        String instagram,

        @Size(max = 255, message = "Naver link must be under 255 characters")
        @Pattern(
                regexp = "^(https?://)?(www\\.)?naver\\.com/.*$",
                message = "Invalid Naver URL format"
        )
        String naver
) {}
