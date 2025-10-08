package com.moden.modenapi.modules.booking.dto;

import com.moden.modenapi.common.enums.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReservationRes(
        UUID id, UUID customerId, UUID designerId, UUID serviceId, UUID studioId,
        ReservationStatus status, Instant reservedAt, String externalRef
) {}