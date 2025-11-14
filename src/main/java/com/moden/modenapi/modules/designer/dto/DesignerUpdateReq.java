package com.moden.modenapi.modules.designer.dto;

import com.moden.modenapi.common.enums.Weekday;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record DesignerUpdateReq(

        @Schema(description = "Designer bio", example = "10년 경력의 컬러 전문 디자이너입니다.")
        String bio,

        @Schema(description = "Portfolio URL (optional)", example = "https://instagram.com/....")
        String portfolioUrl,

        @Schema(description = "Phone number", example = "01012345678")
        String phone,

        @Schema(description = "Position enum name", example = "DESIGNER")
        String position,

        @Schema(description = "Status enum name", example = "WORKING")
        String status,

        @Schema(
                description = "Days off codes (0=MONDAY ... 6=SUNDAY)",
                example = "[0, 6]"
        )
        List<Integer> daysOff
) {}
