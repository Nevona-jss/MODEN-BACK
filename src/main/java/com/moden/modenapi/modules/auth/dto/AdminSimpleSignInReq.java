package com.moden.modenapi.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AdminSimpleSignInReq(
        @NotBlank @Schema(example = "admin")        String fullName,
        @NotBlank @Schema(example = "940223633")    String phone
) {}