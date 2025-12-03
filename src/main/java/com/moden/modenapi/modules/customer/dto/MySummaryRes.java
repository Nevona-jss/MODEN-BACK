package com.moden.modenapi.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "고객 요약 정보 (쿠폰 + 포인트)")
public record MySummaryRes(

        @Schema(description = "사용 가능한 쿠폰 개수")
        byte availableCouponCount,

        @Schema(description = "사용 가능한 포인트 합계")
        BigDecimal availablePointsSum
) {}
