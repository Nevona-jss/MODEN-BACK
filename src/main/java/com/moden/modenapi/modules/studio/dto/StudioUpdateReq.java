package com.moden.modenapi.modules.studio.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for updating hair studio profile fields (partial update).
 * All fields are optional — only non-null values will be applied.
 */
public record StudioUpdateReq(

        @Size(max = 50, message = "Owner phone must be under 50 characters")
        String ownerPhone,

        @Size(max = 50, message = "Studio phone must be under 50 characters")
        String studioPhone,

        @Size(max = 255, message = "Address must be under 255 characters")
        String address,

        @Size(max = 255, message = "Logo URL must be under 255 characters")
        String logo,

        @Size(max = 255, message = "Instagram URL must be under 255 characters")
        @Pattern(
                regexp = "^(https?://)?(www\\.)?instagram\\.com/.*$",
                message = "Invalid Instagram URL format"
        )
        String instagram,

        @Size(max = 255, message = "Naver URL must be under 255 characters")
        @Pattern(
                regexp = "^(https?://)?(www\\.)?naver\\.com/.*$",
                message = "Invalid Naver URL format"
        )
        String naver
) {}
