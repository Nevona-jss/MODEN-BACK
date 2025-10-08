package com.moden.modenapi.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Sign In Request DTO (record)
 * Login faqat telefon raqam orqali amalga oshadi.
 */
public record SignInRequest(
        @NotBlank String name,
        @NotBlank String phone
) {}
