package com.moden.modenapi.modules.qa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "QACreateRequest", description = "Create new inquiry (customer side)")
public record QACreateRequest(

        @NotBlank
        @Size(max = 200)
        @Schema(description = "Inquiry title", example = "두피 케어 상품 문의")
        String title,

        @NotBlank
        @Schema(description = "Inquiry content", example = "어제 받은 두피 클리닉 후 집에서 어떤 샴푸를 써야 할까요?")
        String content
) {}
