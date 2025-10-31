package com.moden.modenapi.modules.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Coupon creation request DTO")
public record CouponCreateReq(
        UUID studioId,
        UUID userId,
        String name,
        BigDecimal discountRate,
        BigDecimal discountAmount,
        boolean birthdayCoupon,
        boolean firstVisitCoupon,
        LocalDate expiryDate
) {}
