package com.moden.modenapi.modules.payment.dto;

import com.moden.modenapi.common.enums.PaymentMethod;
import com.moden.modenapi.common.enums.PaymentStatus;

import java.math.BigDecimal;

public record PaymentUpdateReq(
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        BigDecimal couponDiscount,
        BigDecimal pointsUsed
) {}
