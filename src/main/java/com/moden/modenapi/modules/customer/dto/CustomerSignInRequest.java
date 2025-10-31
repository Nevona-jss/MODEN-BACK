package com.moden.modenapi.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ✅ DTO for customer sign-in via name + phone (no password)
 */
@Schema(description = "Customer sign-in request (by name and phone)")
public record CustomerSignInRequest(

        @Schema(description = "Customer's full name", example = "홍길동")
        String fullName,

        @Schema(description = "Customer's phone number", example = "01012345678")
        String phone
) {}
