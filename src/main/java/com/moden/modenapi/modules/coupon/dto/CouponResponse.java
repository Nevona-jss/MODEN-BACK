package com.moden.modenapi.modules.coupon.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.moden.modenapi.common.enums.CouponStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CouponResponse(
        UUID id,
        UUID studioId,
        UUID userId,
        String name,
        BigDecimal discountRate,
        BigDecimal discountAmount,
        CouponStatus status,

        @JsonProperty("boshlanishSana")
        LocalDate startDate,

        @JsonProperty("tugashSana")
        LocalDate expiryDate,

        @JsonProperty("yaratilganSana")
        Instant createdAt,

        @JsonProperty("yangilanganSana")
        Instant updatedAt,

        boolean birthdayCoupon,
        boolean firstVisitCoupon
) {}
