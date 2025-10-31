package com.moden.modenapi.modules.consult.dto;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.enums.PaymentMethod;
import com.moden.modenapi.common.enums.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record ConsultationRes(
        UUID id,
        UUID serviceId,
        ConsultationStatus status,
        String styleImageUrl,
        String beforeImageUrl,
        String afterImageUrl,
        String consultationMemo,
        String customerMemo,
        String drawingMemoUrl,
        // 🔹 Payment Info
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        Long paymentAmount,
        Instant paymentCreatedAt,
        Instant createdAt,
        Instant updatedAt
) {}
