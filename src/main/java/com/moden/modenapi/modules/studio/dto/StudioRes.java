package com.moden.modenapi.modules.studio.dto;

import java.util.UUID;

/**
 * ✅ StudioRes
 * Returned after creating or retrieving a Hair Studio.
 */
public record StudioRes(
        UUID id,
        String idForLogin,
        String name,
        String businessNo,
        String owner,
        String ownerPhone,
        String studioPhone,
        String address,
        String logo,
        String instagram,
        String naver
) {}
