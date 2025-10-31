package com.moden.modenapi.modules.payment.dto;

import com.moden.modenapi.common.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Payment creation request (includes discounts and points)")
public record PaymentCreateReq(
        UUID serviceId,
        PaymentMethod paymentMethod,
        BigDecimal couponDiscount,
        BigDecimal pointsUsed
) {}
