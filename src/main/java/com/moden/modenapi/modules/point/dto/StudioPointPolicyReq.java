package com.moden.modenapi.modules.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for defining or updating studio’s cashback (point) policy.
 */
@Schema(description = "Request DTO for setting studio point policy")
public record StudioPointPolicyReq(

        @NotNull
        @Schema(description = "Cashback percentage (e.g. 5.0 = 5%)", example = "10.0")
        BigDecimal pointRate
) {}
