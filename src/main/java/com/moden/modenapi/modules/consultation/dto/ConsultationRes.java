package com.moden.modenapi.modules.consultation.dto;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "상담 상세 응답 DTO")
public record ConsultationRes(

        @Schema(description = "상담 ID (UUID)")
        UUID id,

        @Schema(description = "예약 ID (UUID)")
        UUID reservationId,

        @Schema(description = "고객 이름 (Full Name)")
        String customerFullName,

        @Schema(description = "디자이너 이름 (Full Name)")
        String designerFullName,

        @Schema(description = "서비스 이름 (시술명)")
        String serviceName,

        String name, java.math.BigDecimal totalPayment, @Schema(description = "예약 일시 (날짜 + 시간)")
        LocalDateTime reservationAt,

        @Schema(
                description = "상담 상태 (상담대기 / 상담완료 등)",
                implementation = ConsultationStatus.class
        )
        ConsultationStatus status,

        @Schema(
                description = "결제 상태 (예: PENDING / PAID / CANCELED 등)",
                implementation = PaymentStatus.class
        )
        PaymentStatus paymentStatus,


        @Schema(description = "고객이 원하는 스타일 이미지 URL")
        String wantedImageUrl,

        @Schema(description = "시술 전(before) 이미지 URL")
        String beforeImageUrl,

        @Schema(description = "시술 후(after) 이미지 URL")
        String afterImageUrl,

        @Schema(description = "디자이너 상담 메모")
        String consultationMemo,

        @Schema(description = "고객 메모")
        String customerMemo,

        @Schema(description = "그림 메모 이미지 URL")
        String drawingImageUrl,

        @Schema(description = "생성 시각")
        Instant createdAt,

        @Schema(description = "수정 시각")
        Instant updatedAt,

        @Schema(description = "삭제 시각 (soft delete, null 가능)")
        Instant deletedAt
) {}
