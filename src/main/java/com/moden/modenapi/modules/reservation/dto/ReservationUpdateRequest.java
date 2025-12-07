package com.moden.modenapi.modules.reservation.dto;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.enums.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "예약 수정 요청 DTO")
public record ReservationUpdateRequest(

        @Schema(description = "고객 ID (UUID)")
        UUID customerId,

        @Schema(description = "디자이너 ID (UUID)")
        UUID designerId,

        @Schema(description = "선택된 서비스 ID 목록")
        java.util.List<UUID> serviceIds,


        @Schema(description = "consultation ID (UUID)", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID consultationId,

        @Schema(
                description = "상담  상태 (PENDING, COMPLETED 등)",
                implementation = ConsultationStatus.class
        )
        ConsultationStatus consultationStatus,

        @Schema(description = "예약 날짜")
        LocalDate reservationDate,

        @Schema(description = "예약 시작 시간 (HH:mm)")
        String startTime,

        @Schema(description = "예약 종료 시간 (HH:mm)")
        String endTime,

        @Schema(description = "비고 / 설명")
        String description,

        @Schema(description = "예약 상태 (옵션)")
        ReservationStatus status
) {}
