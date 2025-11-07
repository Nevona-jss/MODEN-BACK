package com.moden.modenapi.modules.designer.dto;

import com.moden.modenapi.common.enums.DesignerStatus;
import com.moden.modenapi.common.enums.Position;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Studio creates Designer — server will auto-generate idForLogin and force role=DESIGNER */
public record DesignerCreateDto(

        String bio,
        String portfolioUrl, // (hozir ishlatmasangiz ham qoldirishingiz mumkin)

        // optional full name for User (nullable bo‘lishi mumkin)
        @Size(min = 2, max = 150)
        String fullName,

        // required: unique (users.phone unique constraint bor)
        @NotBlank @Size(max = 20)
        String phone,

        // initial position (optional)
        Position position,

        // password will be hashed to AuthLocal
        @Schema(description = "Login password (plain text). It will be hashed before saving.",
                example = "moden1234!", required = true)
        @NotBlank @Size(min = 8, max = 100)
        String password,

        // optional
        DesignerStatus status
) {}
