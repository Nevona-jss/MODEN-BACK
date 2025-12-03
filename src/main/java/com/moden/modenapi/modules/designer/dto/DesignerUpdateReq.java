package com.moden.modenapi.modules.designer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record DesignerUpdateReq(

        @Schema(
                description = "Portfolio image URLs (optional)",
                example = "[\"https://.../1.jpg\", \"https://.../2.jpg\"]"
        )
        List<String> portfolio,   

        @Schema(description = "Full name", example = "홍길동")
        String fullName,

        @Schema(description = "Login ID (idForLogin)", example = "designer01")
        String idForLogin,

        @Schema(description = "Phone number", example = "01012345678")
        String phone,

        @Schema(description = "Position enum name", example = "DESIGNER")
        String position,

        @Schema(description = "Status enum name", example = "WORKING")
        String status,

        @Schema(
                description = "Days off codes (0=MON..6=SUN)",
                example = "[0, 6]"
        )
        List<Integer> daysOff
) {}
