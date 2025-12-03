package com.moden.modenapi.modules.auth.dto;

public record StudioCreatedResponse(
        String ownerPhone,
        String shopName,
        String idForLogin
) {}
