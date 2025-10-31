package com.moden.modenapi.modules.designer.dto;

import com.moden.modenapi.common.enums.Position;
import com.moden.modenapi.common.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DesignerCreateDto(
        // optional: initial bio/portfolio
        String bio,
        String portfolioUrl,

        // phone (entity has it)
        @Size(max = 20)
        String phone,

        // which studio this designer belongs to
        @Schema(description = "Hair studio id this designer belongs to", required = true)
        UUID hairStudioId,

        // optional login code; generated if missing
        @Schema(description = "Custom login ID (optional). If not provided, will be auto-generated like DS-ABCDE-12345")
        String idForLogin,

        // initial position (optional)
        Position position,

        // password will be hashed to AuthLocal
        @Schema(description = "Login password (plain text). It will be hashed before saving.", example = "moden1234!", required = true)
        @NotBlank @Size(min = 6, max = 100)
        String password,

        Role role
) {}
