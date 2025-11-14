package com.moden.modenapi.modules.qa.dto;

import com.moden.modenapi.common.enums.QAStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "QAResponse", description = "Inquiry + answer response DTO")
public record QAResponse(

        UUID id,

        UUID userId,                    // 문의 작성자 userId

        @Schema(description = "Customer name (from User.fullName)")
        String customerName,

        @Schema(description = "Customer phone (from User.phone)")
        String customerPhone,

        String title,
        String content,

        QAStatus status,                // PENDING / ANSWERED / CLOSED

        String answer,

        @Schema(description = "Name of answer author (studio/designer fullName)")
        String answeredByName,

        Instant createdAt,
        Instant answeredAt
) {}
