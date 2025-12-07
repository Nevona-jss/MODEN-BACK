package com.moden.modenapi.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "결제 시 선택한 상품 한 줄 (상품 + 수량)")
public record PaymentProductLineReq(

        @Schema(description = "상품 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID productId,

        @Schema(description = "수량", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer quantity
) {}
