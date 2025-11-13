package com.moden.modenapi.modules.coupon.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CouponCreateRequest(
        @NotNull UUID studioId,
        @NotNull UUID userId,
        @Size(max = 100) String name,

        @DecimalMin(value = "0.01", message = "discountRate 0 dan katta bo‘lishi kerak")
        @DecimalMax(value = "100.00", message = "discountRate 100 dan oshmasligi kerak")
        BigDecimal discountRate,

        @DecimalMin(value = "0.01", message = "discountAmount 0 dan katta bo‘lishi kerak")
        BigDecimal discountAmount,

        LocalDate startDate,
        LocalDate expiryDate,

        boolean birthdayCoupon,
        boolean firstVisitCoupon,

        boolean isGlobal
) {}
