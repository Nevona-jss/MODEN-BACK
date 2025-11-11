package com.moden.modenapi.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * ✅ CustomerSignUpRequest
 * 고객 회원가입 요청 DTO (이름 + 전화번호 + 선택적 스튜디오 ID + 고정 Role)
 */

public record CustomerSignUpRequest(
        @Schema(example = "Harry Potter") @NotBlank String fullName,
        @Schema(example = "01012345678") @NotBlank String phone
) {}

