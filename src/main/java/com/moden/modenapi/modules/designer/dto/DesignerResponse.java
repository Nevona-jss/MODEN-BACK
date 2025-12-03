package com.moden.modenapi.modules.designer.dto;

import com.moden.modenapi.common.enums.DesignerStatus;
import com.moden.modenapi.common.enums.Position;
import com.moden.modenapi.common.enums.Weekday;
import java.util.List;
import java.util.UUID;

public record DesignerResponse(
        UUID userId,
        String idForLogin,    // DS-XXXXX-12345
        String fullName,      // ✅ USER.fullName
        String phone,         // ✅ USER.phone
        Position position,    // DESINGER / MANAGER ...
        DesignerStatus status,// WORKING / LEAVE ...
        List<Weekday> daysOff,// 쉬는 요일 리스트
        List<String> portfolio
) {}
