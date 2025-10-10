package com.moden.modenapi.modules.booking.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record ReservationCreateReq(
        @NotNull UUID customerId,
        @NotNull UUID designerId,
        @NotNull UUID serviceId,
        @NotNull UUID studioId,
        Instant reservedAt,
        String externalRef
) {}
