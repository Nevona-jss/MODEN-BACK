package com.moden.modenapi.modules.qa.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "QACreateRequest", description = "Create new inquiry (customer side)")
public record QACreateRequest(

        @Schema(description = "Inquiry title", example = "예약 관련 문의", required = true)
        @NotBlank String title,

        @Schema(description = "Inquiry content", example = "예약 취소는 어디서 하나요?", required = true)
        @NotBlank String content
) {}
