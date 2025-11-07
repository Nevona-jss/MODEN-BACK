package com.moden.modenapi.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginReqStudioAndDesigner(

        @Schema(example = "ST-MONA-806743") @NotBlank String idForLogin,
        @Schema(example = "studio123!")     @NotBlank String password

) {
}
