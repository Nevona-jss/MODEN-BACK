package com.moden.modenapi.modules.payment.dto;

import com.moden.modenapi.common.enums.PaymentMethod;
import com.moden.modenapi.common.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결제 수정 요청")
public record PaymentUpdateReq(

        @Schema(description = "결제 상태 (UNPAID / PAID / CANCELED ...)")
        PaymentStatus paymentStatus,

        @Schema(description = "결제 수단 (변경용)")
        PaymentMethod paymentMethod
) {}
