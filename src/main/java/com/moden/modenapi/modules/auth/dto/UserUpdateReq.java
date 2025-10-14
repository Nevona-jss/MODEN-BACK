package com.moden.modenapi.modules.auth.dto;

import com.moden.modenapi.common.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * ✅ UserUpdateReq
 * DTO for updating user profile information.
 *
 * - email (optional, validated format)
 * - birthdate (must be a past date)
 * - gender (enum)
 * - consentMarketing (nullable Boolean)
 */
public record UserUpdateReq(

        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email length must be under 255 characters")

        String email,

        @Past(message = "Birthdate must be in the past")
        LocalDate birthdate,

        Gender gender,

        Boolean consentMarketing ,// ⚙️ made nullable so user can skip this field,

        @Size(max = 255, message = "Address must be under 255 characters")
        String address
) {}
