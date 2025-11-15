package com.moden.modenapi.modules.consultation.dto;

import com.moden.modenapi.common.enums.ConsultationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상담 수정 요청 DTO")
public record ConsultationUpdateReq(

        @Schema(
                description = "상담 상태 (상담대기/상담완료 등 – enum 값)",
                implementation = ConsultationStatus.class
        )
        ConsultationStatus status,

        @Schema(
                description = "고객이 원하는 스타일 이미지 URL"
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
                description = "디자이너 상담 메모",
                example = "추가로 모발 상태 체크 후 케어 권장."
        )
        String consultationMemo,

        @Schema(
                description = "고객 메모",
                example = "두피가 약하니 자극 적게 부탁드려요."
        )
        String customerMemo,

        @Schema(
                description = "그림 메모 이미지 URL"
        )
        String drawingImageUrl
) {}
