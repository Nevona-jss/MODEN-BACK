package com.moden.modenapi.modules.designer.dto;

import com.moden.modenapi.common.enums.DesignerStatus;
import com.moden.modenapi.common.enums.Position;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.enums.Weekday;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DesignerResponse(
        UUID userId,          // user.id
        UUID studioId,        // hair_studio_id
        String idForLogin,    // DS-XXXXX-12345

        Role role,            // USER.role  (DESIGNER)
        String fullName,      // ✅ USER.fullName
        String phone,         // ✅ USER.phone

        Position position,    // DESINGER / MANAGER ...
        DesignerStatus status,// WORKING / LEAVE ...
        List<Weekday> daysOff,// 쉬는 요일 리스트

        List<PortfolioItemRes> portfolio, // 포트폴리오
        Instant createdAt,
        Instant updatedAt
) {}
