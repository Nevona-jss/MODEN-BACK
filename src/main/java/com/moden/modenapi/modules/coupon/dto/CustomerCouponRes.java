package com.moden.modenapi.modules.coupon.dto;

import com.moden.modenapi.common.enums.CouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "고객이 보유한 쿠폰 정보 응답 DTO")
public record CustomerCouponRes(

        @Schema(description = "고객 쿠폰 ID")
        UUID id,

        @Schema(description = "헤어샵(스튜디오) ID")
        UUID studioId,

        @Schema(description = "쿠폰 ID")
        UUID couponId,

        @Schema(description = "쿠폰 이름")
        String couponName,

        @Schema(description = "쿠폰 설명")
        String description,

        @Schema(description = "할인율(%)")
        BigDecimal discountRate,

        @Schema(description = "정액 할인 금액")
        BigDecimal discountAmount,

        @Schema(description = "쿠폰 상태")
        CouponStatus status,

        @Schema(description = "쿠폰 사용 시작일")
        LocalDate startDate,

        @Schema(description = "쿠폰 사용 종료일(만료일)")
        LocalDate expiryDate,

        @Schema(description = "고객에게 쿠폰이 발급된 시간")
        Instant issuedAt,

        @Schema(description = "쿠폰 사용 시간(사용된 경우)")
        Instant usedDate
) {}
