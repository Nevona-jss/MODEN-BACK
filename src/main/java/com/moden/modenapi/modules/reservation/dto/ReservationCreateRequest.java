package com.moden.modenapi.modules.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "예약 생성 요청 DTO")
public record ReservationCreateRequest(

        @Schema(description = "고객 ID (UUID)", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID customerId,    // 실제 생성 시에는 currentUserId 로 덮어쓸 예정 (옵션)

        @Schema(description = "디자이너 ID (UUID)", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID designerId,

        @Schema(description = "서비스 ID (UUID)", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID serviceId,

        @Schema(
                description = "예약 일시 (날짜 + 시간)",
                example = "2025-11-20T14:30:00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalDateTime reservationAt,

        @Schema(description = "비고 / 설명")
        String description
) {}
