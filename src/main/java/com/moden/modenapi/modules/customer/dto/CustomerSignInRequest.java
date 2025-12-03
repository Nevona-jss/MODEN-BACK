package com.moden.modenapi.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ✅ DTO for customer sign-in via name + phone (no password)
 */
@Schema(description = "Customer sign-in request (by name and phone)")
public record CustomerSignInRequest(

        @Schema(description = "Customer's full name", example = "Harry")
        String fullName,

        @Schema(description = "Customer's phone number", example = "123456789")
        String phone
) {}
