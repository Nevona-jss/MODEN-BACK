package com.moden.modenapi.modules.payment.dto;

import com.moden.modenapi.common.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "결제 목록용 요약 DTO")
public record PaymentListItemRes(

        @Schema(description = "결제 ID")
        UUID paymentId,

        @Schema(description = "예약 ID")
        UUID reservationId,

        @Schema(description = "상담완료 일시 (예약 완료 일시)")
        Instant consultCompletedAt,

        @Schema(description = "고객 이름 (fullName)")
        String customerFullName,

        @Schema(description = "디자이너 이름 (fullName)")
        String designerFullName,

        @Schema(description = "서비스 이름")
        String serviceName,

        @Schema(description = "최종 결제 금액 (서비스 + 제품 - 포인트 - 쿠폰)")
        BigDecimal totalAmount,

        @Schema(description = "결제 상태")
        PaymentStatus paymentStatus,

        @Schema(description = "디자이너 인센티브 금액")
        BigDecimal designerTipAmount
) {}
