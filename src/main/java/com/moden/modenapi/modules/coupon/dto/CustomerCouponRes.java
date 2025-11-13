package com.moden.modenapi.modules.coupon.dto;

import com.moden.modenapi.common.enums.CouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerCouponRes(

        @Schema(description = "CustomerCoupon ID")
        UUID id,

        @Schema(description = "Studio ID")
        UUID studioId,

        @Schema(description = "Coupon ID")
        UUID couponId,

        @Schema(description = "Coupon nomi")
        String couponName,

        @Schema(description = "Chegirma (%)")
        BigDecimal discountRate,

        @Schema(description = "Chegirma (fixed amount)")
        BigDecimal discountAmount,

        @Schema(description = "Coupon holati")
        CouponStatus status,

        @Schema(description = "Coupon boshlanish sanasi")
        LocalDate startDate,

        @Schema(description = "Coupon tugash sanasi")
        LocalDate expiryDate,

        @Schema(description = "Customerga berilgan vaqt")
        Instant issuedAt,

        @Schema(description = "Ishlatilgan vaqt (bo'lsa)")
        Instant usedAt
) {}
