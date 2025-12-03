package com.moden.modenapi.modules.payment.dto;

import com.moden.modenapi.common.enums.PaymentMethod;
import com.moden.modenapi.common.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "결제 상세 응답 DTO")
public record PaymentRes(

        @Schema(description = "결제 ID")
        UUID id,

        @Schema(description = "예약 ID")
        UUID reservationId,

        @Schema(description = "결제 상태 (UNPAID / PAID ...)")
        PaymentStatus paymentStatus,

        @Schema(description = "결제 수단 (CASH / CARD ...)")
        PaymentMethod paymentMethod,

        @Schema(description = "서비스(시술) 총 금액")
        BigDecimal serviceTotal,

        @Schema(description = "제품 사용 총 금액")
        BigDecimal productTotal,

        @Schema(description = "쿠폰 할인 금액 합계 (계산값)")
        BigDecimal couponDiscount,

        @Schema(description = "포인트 사용 금액")
        BigDecimal pointsUsed,

        @Schema(description = "최종 결제 금액 (실제 지불액)")
        BigDecimal totalAmount,

        @Schema(description = "디자이너 인센티브 금액")
        BigDecimal designerTipAmount,

        @Schema(description = "생성 시각")
        Instant createdAt,

        @Schema(description = "수정 시각")
        Instant updatedAt
) {}
