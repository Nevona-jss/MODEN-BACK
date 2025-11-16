package com.moden.modenapi.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "디자이너별 팁 요약 DTO")
public record DesignerTipSummaryRes(

        @Schema(description = "디자이너 ID")
        UUID designerId,

        @Schema(description = "서비스 기준 팁 합계")
        BigDecimal serviceTipTotal,

        @Schema(description = "전체 팁 합계 (현재는 서비스 팁과 동일)")
        BigDecimal totalTip
) {}
