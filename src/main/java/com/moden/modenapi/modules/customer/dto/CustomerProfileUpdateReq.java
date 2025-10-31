package com.moden.modenapi.modules.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CustomerProfileUpdateReq(
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email length must be under 255 characters")
        String email,

        @Past(message = "Birthdate must be in the past")
        LocalDate birthdate,

        @Size(max = 10, message = "Gender length must be under 10 characters")
        String gender,

        Boolean consentMarketing,

        @Size(max = 255, message = "Address length must be under 255 characters")
        String address,

        @Size(max = 255, message = "Image URL length must be under 255 characters")
        String profileImageUrl,

        @Size(max = 500, message = "Visit reason must be under 500 characters")
        String visitReason
) {}
