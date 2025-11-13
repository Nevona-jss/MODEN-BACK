package com.moden.modenapi.modules.designer.dto;

import com.moden.modenapi.common.enums.Position;
import com.moden.modenapi.common.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record DesignerResponse(
        UUID id,          // 1
        UUID userId,      // 2
        UUID studioId,    // 3
        String idForLogin,// 4

        Role role,        // 5 ✅ enum Role
        String phone,     // 6 ✅ 전화번호
        Position position,// 7

        String bio,                   // 8
        List<PortfolioItemRes> portfolio, // 9
        Instant createdAt,            // 10
        Instant updatedAt             // 11
) {}
