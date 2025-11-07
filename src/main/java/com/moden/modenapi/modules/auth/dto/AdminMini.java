package com.moden.modenapi.modules.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminMini(
        UUID userId,
        String fullName,
        String phone,
        String role,
        Instant createdAt
) {}
