package com.moden.modenapi.modules.consult.dto;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.enums.PaymentMethod;
import com.moden.modenapi.common.enums.PaymentStatus;
import java.util.UUID;

public record ConsultationCreateReq(
        UUID serviceId,
        ConsultationStatus status,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        String styleImageUrl,
        String beforeImageUrl,
        String afterImageUrl,
        String consultationMemo,
        String customerMemo,
        String drawingMemoUrl
) {}
