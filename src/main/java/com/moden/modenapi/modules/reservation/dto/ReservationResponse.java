package com.moden.modenapi.modules.reservation.dto;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.enums.PaymentStatus;
import com.moden.modenapi.common.enums.ConsultationStatus;   // ✅ 새 enum (WAITING/COMPLETED)
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

        @Schema(description = "고객 이름(풀네임)")
        String customerFullName,

        @Schema(description = "디자이너 ID (UUID)")
        UUID designerId,

        @Schema(description = "디자이너 이름(풀네임)")
        String designerFullName,

        String serviceName,

        @Schema(description = "예약 일시 (날짜 + 시간)")
        LocalDateTime reservationAt,

        @Schema(description = "비고 / 설명")
        String description,

        @Schema(
                description = "예약 상태 (RESERVED / CANCELED / COMPLETED 등)",
                implementation = ReservationStatus.class
        )
        ReservationStatus status,

        @Schema(
                description = "상담 상태 (WAITING=상담대기 / COMPLETED=상담완료)",
                implementation = ConsultationStatus.class
        )
        ConsultationStatus consultationStatus,

        @Schema(description = "결제 ID (PG사에서 내려주는 payment id 등)")
        String paymentId,

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

){}
