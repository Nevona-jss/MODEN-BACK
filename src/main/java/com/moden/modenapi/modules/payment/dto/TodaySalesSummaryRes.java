package com.moden.modenapi.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "오늘 매출 요약 DTO")
public record TodaySalesSummaryRes(

        @Schema(description = "기준 날짜 (오늘)")
        LocalDate date,

        @Schema(description = "오늘 총 매출 (PAID 결제 합계)")
        BigDecimal totalSales,

        @Schema(description = "오늘 결제 건수 (PAID)")
        long paymentCount,

        @Schema(description = "평균 결제 단가 (총 매출 / 건수)")
        BigDecimal averageAmount
) {}
