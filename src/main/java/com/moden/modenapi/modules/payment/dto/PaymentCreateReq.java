package com.moden.modenapi.modules.payment.dto;

import com.moden.modenapi.common.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "오프라인 결제 확정 요청 (포인트/쿠폰 적용)")
public record PaymentCreateReq(

        @Schema(description = "예약 ID (UUID)", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID reservationId,

        @Schema(
                description = "선택한 제품(샴푸, 트리트먼트 등) 총 금액",
                example = "30000"
        )
        BigDecimal productTotal,

        @Schema(
                description = "사용할 포인트 금액",
                example = "5000"
        )
        BigDecimal pointsToUse,

        @Schema(
                description = "사용할 쿠폰 ID (고객용/스튜디오 공용 쿠폰 모두 가능, 선택값)",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        UUID couponId,

        @Schema(
                description = "결제 수단 (CASH / CARD / ...)",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        PaymentMethod paymentMethod
) {}
