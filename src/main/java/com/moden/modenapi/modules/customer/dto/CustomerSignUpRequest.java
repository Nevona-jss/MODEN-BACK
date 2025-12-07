package com.moden.modenapi.modules.customer.dto;

import com.moden.modenapi.common.enums.Gender;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.modules.studio.dto.StudioBrief;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * ✅ CustomerSignUpRequest
 * 고객 회원가입 요청 DTO (이름 + 전화번호 + 선택적 스튜디오 ID + 고정 Role)
 */

public record CustomerSignUpRequest(
        @Schema(example = "Harry Potter") @NotBlank String fullName,

        @Schema(example = "01012345678") @NotBlank String phone,

        @Schema(description = "상담 담당 디자이너 ID (UUID, 옵션)")
        String designerId,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email length must be under 255 characters")
        String email,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Past(message = "Birthdate must be in the past")
        LocalDate birthdate,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, allowableValues = {"MALE", "FEMALE", "OTHER"})
        Gender gender,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "마케팅 수신 동의 여부")
        Boolean consentMarketing,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 255, message = "Address length must be under 255 characters")
        String address,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 500, message = "Visit reason must be under 500 characters")
        String visitReason

) {}
