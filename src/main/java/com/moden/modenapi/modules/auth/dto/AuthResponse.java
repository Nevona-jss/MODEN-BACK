package com.moden.modenapi.modules.auth.dto;

/**
 * Auth Response DTO (record)
 * JWT token bilan foydalanuvchi ma’lumotlarini qaytaradi.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken
) {}
