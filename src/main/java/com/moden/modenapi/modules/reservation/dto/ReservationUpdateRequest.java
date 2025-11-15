package com.moden.modenapi.modules.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "예약 수정 요청 DTO")
public record ReservationUpdateRequest(

        @Schema(description = "고객 ID (UUID) — 보통은 변경하지 않음")
        UUID customerId,

        @Schema(description = "디자이너 ID (UUID)")
        UUID designerId,

        @Schema(description = "서비스 ID (UUID)")
        UUID serviceId,

        @Schema(
                description = "예약 일시 (옵션)",
                example = "2025-11-20T16:00:00"
        )
        LocalDateTime reservationAt,

        @Schema(description = "비고 / 설명")
        String description
) {}
