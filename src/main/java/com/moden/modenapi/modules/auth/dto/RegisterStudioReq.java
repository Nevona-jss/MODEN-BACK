package com.moden.modenapi.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterStudioReq(
        @NotBlank String ownerPhone,
        @NotBlank String password,
        @NotBlank String shopName,
        @NotBlank String businessNo,
        @NotBlank String ownerName

) {
}
