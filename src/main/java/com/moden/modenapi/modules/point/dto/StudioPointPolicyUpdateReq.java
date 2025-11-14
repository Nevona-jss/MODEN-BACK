package com.moden.modenapi.modules.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Studio point policy update request")
public record StudioPointPolicyUpdateReq(

        @Schema(
                description = "Point earn rate (percent). Example: 5.00 means 5%",
                example = "5.00"
        )
        BigDecimal pointRate
) {}
