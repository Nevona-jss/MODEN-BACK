package com.moden.modenapi.modules.consultation.dto;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.enums.PaymentStatus;
import com.moden.modenapi.modules.consultation.model.Consultation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder
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

        @Schema(description = "디자이너 직위")
        String designerPosition,

        @Schema(description = "헤어 스튜디오 이름")
        String studioName,

        @Schema(description = "총 결제 금액")
        BigDecimal totalPayment,

        @Schema(description = "예약 날짜")
        LocalDate reservationDate,

        @Schema(description = "예약 시작 시간")
        String startTime,

        @Schema(description = "예약 종료 시간")
        String endTime,

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
) {
    public static ConsultationRes from(Consultation c) {
        return ConsultationRes.builder()
                .id(c.getId())
                .reservationId(c.getReservationId())
                .status(c.getStatus())
                .beforeImageUrl(c.getBeforeImageUrl())
                .afterImageUrl(c.getAfterImageUrl())
                .consultationMemo(c.getConsultationMemo())
                .build();
    }
}
