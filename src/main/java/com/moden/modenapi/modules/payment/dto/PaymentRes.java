package com.moden.modenapi.modules.payment.dto;

import com.moden.modenapi.common.enums.PaymentMethod;
import com.moden.modenapi.common.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRes(
        UUID id,
        UUID serviceId,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        BigDecimal servicePrice,
        BigDecimal productTotal,
        BigDecimal couponDiscount,
        BigDecimal pointsUsed,
        BigDecimal amount,
        Instant createdAt,
        Instant updatedAt
) {}
