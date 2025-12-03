package com.moden.modenapi.modules.coupon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moden.modenapi.common.enums.CouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;


@Schema(description = "쿠폰 상세 응답 DTO")
public record CouponFirstRegisterRes(

        @Schema(description = "쿠폰 ID")
        UUID id,

        @Schema(description = "헤어샵(스튜디오) ID")
        UUID studioId,

        @Schema(description = "쿠폰 이름")
        String name,

        @Schema(description = "할인율(%)")
        BigDecimal discountRate,

        @Schema(description = "정액 할인 금액")
        BigDecimal discountAmount,

        @Schema(description = "쿠폰 상태")
        CouponStatus status,

        @Schema(description = "쿠폰 설명")
        String description,

        @Schema(description = "쿠폰 사용 시작일")
        @JsonProperty("startDate")
        LocalDate startDate,

        @Schema(description = "쿠폰 만료일(사용 종료일)")
        @JsonProperty("expiryDate")
        LocalDate expiryDate,

        @Schema(description = "쿠폰 생성 시각")
        @JsonProperty("createdAt")
        Instant createdAt,

        @Schema(description = "쿠폰 최종 수정 시각")
        @JsonProperty("updatedAt")
        Instant updatedAt
) {}
