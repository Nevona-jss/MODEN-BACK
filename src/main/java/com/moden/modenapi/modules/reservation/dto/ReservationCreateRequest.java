package com.moden.modenapi.modules.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moden.modenapi.common.enums.ConsultationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "예약 생성 요청 DTO")
public record ReservationCreateRequest(

        @Schema(description = "선택된 서비스 ID 목록")
        java.util.List<UUID> serviceIds,

        @Schema(description = "고객 ID (UUID)", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID customerId,

        @Schema(description = "디자이너 ID (UUID)", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID designerId,

        @Schema(description = "studio ID (UUID)", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID studioId,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @Schema(description = "예약 날짜", example = "2025-12-22", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate reservationDate,

        @Schema(description = "예약 시작 시간 (HH:mm)", example = "08:00", requiredMode = Schema.RequiredMode.REQUIRED)
        String startTime,

        @Schema(description = "예약 종료 시간 (HH:mm)", example = "09:15", requiredMode = Schema.RequiredMode.REQUIRED)
        String endTime,

        @Schema(description = "비고 / 설명")
        String description
) {}
