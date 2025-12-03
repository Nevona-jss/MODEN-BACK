package com.moden.modenapi.modules.coupon.dto;

import jakarta.validation.constraints.*;
import com.moden.modenapi.common.enums.CouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 쿠폰 부분(부분 업데이트) 수정 요청 DTO.
 * 모든 필드는 선택값이며, 전달된 값만 변경됩니다.
 */
@Schema(description = "쿠폰 부분 수정 요청 DTO (PATCH). 전달된 값만 수정됩니다.")
public record CouponUpdateRequest(

        @Schema(description = "쿠폰 이름 (최대 100자)")
        @Size(max = 100, message = "이름은 100자를 넘을 수 없습니다.")
        String name,

        // % 할인: 0.01 ~ 100.00 (선택)
        @Schema(description = "할인율(%) - 0.01 이상 100.00 이하")
        @DecimalMin(value = "0.01", message = "할인율은 0보다 커야 합니다.")
        @DecimalMax(value = "100.00", message = "할인율은 100을 초과할 수 없습니다.")
        BigDecimal discountRate,

        // 금액 할인: > 0 (선택)
        @Schema(description = "정액 할인 금액 - 0보다 커야 함")
        @DecimalMin(value = "0.01", message = "할인 금액은 0보다 커야 합니다.")
        BigDecimal discountAmount,

        @Schema(description = "쿠폰 설명")
        String description,

        @Schema(description = "쿠폰 사용 시작일 (선택)")
        LocalDate startDate,   // 선택

        @Schema(description = "쿠폰 사용 종료일/만료일 (선택)")
        LocalDate expiryDate,  // 선택

        @Schema(description = "쿠폰 상태 (AVAILABLE / USED / EXPIRED)")
        CouponStatus status
) {}
