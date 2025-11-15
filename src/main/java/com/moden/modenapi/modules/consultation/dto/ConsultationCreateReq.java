package com.moden.modenapi.modules.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "상담 생성 요청 DTO (예약 기반)")
public record ConsultationCreateReq(

        @Schema(
                description = "예약 ID (reservation.id, UUID)",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID reservationId,

        @Schema(
                description = "고객이 원하는 스타일 이미지 URL (/api/universalUploads 로 업로드 후 받은 URL)"
        )
        String wantedImageUrl,

        @Schema(
                description = "시술 전(before) 이미지 URL"
        )
        String beforeImageUrl,

        @Schema(
                description = "시술 후(after) 이미지 URL"
        )
        String afterImageUrl,

        @Schema(
                description = "디자이너 상담 메모 (상담 내용)",
                example = "고객이 밝은 브라운 톤을 원함. 모질이 손상되어 있어 케어 필요."
        )
        String consultationMemo,

        @Schema(
                description = "고객 메모 (고객이 남기는 요청사항)",
                example = "극단적인 탈색은 피하고 싶어요."
        )
        String customerMemo,

        @Schema(
                description = "그림 메모 이미지 URL (SVG/PNG URL)"
        )
        String drawingImageUrl
) {}
