package com.moden.modenapi.modules.qa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "QAAnswerRequest", description = "Answer to customer inquiry (studio/designer)")
public record QAAnswerRequest(

        @NotBlank
        @Schema(description = "Answer content", example = "안녕하세요 고객님, 어제 시술 기준으로 약산성 샴푸를 추천드립니다.")
        String answer
) {}
