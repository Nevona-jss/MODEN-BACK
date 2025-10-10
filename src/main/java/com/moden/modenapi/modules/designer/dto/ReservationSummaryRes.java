package com.moden.modenapi.modules.designer.dto;

import com.moden.modenapi.common.enums.ReservationStatus;
import java.time.Instant;
import java.util.UUID;

public record ReservationSummaryRes(
        UUID reservationId,
        UUID customerId,
        String customerName,
        ReservationStatus status,
        Instant reservedAt
) {}
