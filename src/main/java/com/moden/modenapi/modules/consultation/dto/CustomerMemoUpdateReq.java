package com.moden.modenapi.modules.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "고객 메모 수정 요청 DTO")
public record CustomerMemoUpdateReq(

        @Schema(
                description = "고객 메모 내용",
                example = "다음 방문 때도 같은 톤으로 하고 싶어요."
        )
        String customerMemo
) {}
