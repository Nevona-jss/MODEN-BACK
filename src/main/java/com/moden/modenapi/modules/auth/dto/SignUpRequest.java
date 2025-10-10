package com.moden.modenapi.modules.auth.dto;

import com.moden.modenapi.common.enums.UserType;
import jakarta.validation.constraints.NotBlank;

/**
 * Sign Up Request DTO (record)
 * Foydalanuvchi faqat ism va telefon raqam, usertype bilan ro‘yxatdan o‘tadi.
 */
public record SignUpRequest(
        @NotBlank String name,
        @NotBlank String phone,
        @NotBlank UserType userType
) {}
