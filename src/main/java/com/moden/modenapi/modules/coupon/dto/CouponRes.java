package com.moden.modenapi.modules.coupon.dto;

import com.moden.modenapi.common.enums.CouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Coupon response DTO")
public record CouponRes(
        UUID id,
        UUID studioId,
        UUID userId,
        String name,
        BigDecimal discountRate,
        BigDecimal discountAmount,
        CouponStatus status,
        boolean birthdayCoupon,
        boolean firstVisitCoupon,
        LocalDate expiryDate,
        Instant createdAt,
        Instant updatedAt
) {}
