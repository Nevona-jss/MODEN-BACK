package com.moden.modenapi.modules.reservation.dto;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.enums.PaymentStatus;   // ✅ 추가
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "예약 응답 DTO")
public record ReservationResponse(

        @Schema(description = "예약 ID (UUID)")
        UUID id,

        @Schema(description = "고객 ID (UUID)")
        UUID customerId,

        @Schema(description = "디자이너 ID (UUID)")
        UUID designerId,

        @Schema(description = "서비스 ID (UUID)")
        UUID serviceId,

        @Schema(description = "예약 일시 (날짜 + 시간)")
        LocalDateTime reservationAt,

        @Schema(description = "비고 / 설명")
        String description,

        @Schema(
                description = "예약 상태 (RESERVED / CANCELED)",
                implementation = ReservationStatus.class
        )
        ReservationStatus status,

        @Schema(
                description = "결제 상태 (UNPAID / PAID)",
                implementation = PaymentStatus.class
        )
        PaymentStatus paymentStatus,

        @Schema(description = "생성 시각 (감사 로그)")
        Instant createdAt,

        @Schema(description = "수정 시각 (감사 로그)")
        Instant updatedAt,

        @Schema(description = "삭제 시각 (Soft delete, null 가능)")
        Instant deletedAt

) {}
