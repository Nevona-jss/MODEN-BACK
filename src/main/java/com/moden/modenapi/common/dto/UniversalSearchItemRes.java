package com.moden.modenapi.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "통합 검색 결과 아이템")
public record UniversalSearchItemRes(

        @Schema(description = "타입 (SERVICE / PRODUCT / DESIGNER / STUDIO 등)")
        String type,

        @Schema(description = "엔티티 ID (UUID)")
        UUID id,

        @Schema(description = "제목 (이름)")
        String title,

        @Schema(description = "부제목 / 설명")
        String subtitle,

        @Schema(description = "썸네일 이미지 URL (옵션)")
        String thumbnailUrl
) { }
