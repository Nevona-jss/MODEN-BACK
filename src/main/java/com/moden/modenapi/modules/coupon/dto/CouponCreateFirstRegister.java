package com.moden.modenapi.modules.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "쿠폰 생성 요청 DTO for first register")
public record CouponCreateFirstRegister(

        @Schema(description = "헤어샵(스튜디오) ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "studioId는 필수 값입니다.")
        UUID studioId,

        @Schema(description = "쿠폰 이름 (최대 100자)")
        @Size(max = 100, message = "이름은 100자를 넘을 수 없습니다.")
        String name,

        @Schema(description = "할인율(%) - 0.01 이상 100.00 이하")
        @DecimalMin(value = "0.01", message = "할인율은 0보다 커야 합니다.")
        @DecimalMax(value = "100.00", message = "할인율은 100을 초과할 수 없습니다.")
        BigDecimal discountRate,     // ❗ @NotNull YO‘Q → null ruxsat

        @Schema(description = "정액 할인 금액 - 0보다 커야 함")
        @DecimalMin(value = "0.01", message = "할인 금액은 0보다 커야 합니다.")
        BigDecimal discountAmount,   // ❗ @NotNull YO‘Q → null ruxsat

        @Schema(description = "쿠폰 설명")
        String description,

        @Schema(description = "쿠폰 사용 시작일")
        LocalDate startDate,

        @Schema(description = "쿠폰 만료일(사용 종료일)")
        LocalDate expiryDate
) {}
