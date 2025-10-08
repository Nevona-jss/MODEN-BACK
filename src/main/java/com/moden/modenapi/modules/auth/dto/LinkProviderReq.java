package com.moden.modenapi.modules.auth.dto;


import jakarta.validation.constraints.NotBlank;

public record LinkProviderReq(
        @NotBlank String provider,
        @NotBlank String providerUid
) {}