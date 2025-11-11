package com.moden.modenapi.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Product update request (null은 미변경)")
public record StudioProductUpdateReq(
        String productName,
        BigDecimal price,
        String notes,
        BigDecimal volumeLiters,
        BigDecimal designerTipPercent
) {}
