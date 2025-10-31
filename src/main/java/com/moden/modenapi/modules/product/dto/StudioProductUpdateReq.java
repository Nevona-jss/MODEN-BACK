package com.moden.modenapi.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Product update request")
public record StudioProductUpdateReq(
        String name,
        String category,
        String type,
        BigDecimal price,
        Integer stock,
        String image
) {}
