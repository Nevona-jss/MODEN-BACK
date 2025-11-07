package com.moden.modenapi.modules.customer.dto;

import com.moden.modenapi.common.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CustomerProfileUpdateReq(
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email length must be under 255 characters")
        String email,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Past(message = "Birthdate must be in the past")
        LocalDate birthdate,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, allowableValues = {"MALE","FEMALE","OTHER"})
        Gender gender,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean consentMarketing,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 255, message = "Address length must be under 255 characters")
        String address,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 255, message = "Image URL length must be under 255 characters")
        String profileImageUrl,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 500, message = "Visit reason must be under 500 characters")
        String visitReason
) {}
