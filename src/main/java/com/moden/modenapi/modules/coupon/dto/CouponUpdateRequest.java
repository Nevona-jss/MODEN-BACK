package com.moden.modenapi.modules.coupon.dto;

import jakarta.validation.constraints.*;
import com.moden.modenapi.common.enums.CouponStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * QISMAN yangilash uchun DTO.
 * Barcha maydonlar ixtiyoriy (nullable). Yuborilganlari qo'llanadi.
 */
public record CouponUpdateRequest(

        @Size(max = 100, message = "name uzunligi 100 dan oshmasin")
        String name,

        // % chegirma: 0.01 ~ 100.00 (ixtiyoriy)
        @DecimalMin(value = "0.01", message = "discountRate 0 dan katta bo‘lishi kerak")
        @DecimalMax(value = "100.00", message = "discountRate 100 dan oshmasligi kerak")
        BigDecimal discountRate,

        // pul miqdori: > 0 (ixtiyoriy)
        @DecimalMin(value = "0.01", message = "discountAmount 0 dan katta bo‘lishi kerak")
        BigDecimal discountAmount,

        LocalDate startDate,   // ixtiyoriy
        LocalDate expiryDate,  // ixtiyoriy

        CouponStatus status,   // AVAILABLE / USED / EXPIRED

        Boolean birthdayCoupon,    // ixtiyoriy (null = o‘zgarmasin)
        Boolean firstVisitCoupon   // ixtiyoriy
) {}
