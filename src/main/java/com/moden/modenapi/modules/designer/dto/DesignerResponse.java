package com.moden.modenapi.modules.designer.dto;

import com.moden.modenapi.common.enums.DesignerStatus;
import com.moden.modenapi.common.enums.Position;
import com.moden.modenapi.common.enums.Weekday;
import java.util.List;
import java.util.UUID;

public record DesignerResponse(
        UUID userId,
        UUID ownerUserId,
        String idForLogin,
        String fullName,
        String phone,
        Position position,
        String role,
        DesignerStatus status,
        List<Weekday> daysOff,
        List<String> portfolio
) {}
