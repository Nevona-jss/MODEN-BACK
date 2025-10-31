package com.moden.modenapi.modules.qa.dto;

import com.moden.modenapi.common.enums.QAStatus;

import java.time.Instant;
import java.util.UUID;

public record QAResponse(
        UUID id,
        UUID userId,
        String title,
        String content,
        QAStatus status,
        String answer,
        Instant answeredAt,
        Instant createdAt
) {}
