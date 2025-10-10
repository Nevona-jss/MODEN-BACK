package com.moden.modenapi.modules.point.dto;

import com.moden.modenapi.modules.point.model.PointLedger;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a single point transaction.
 */
public record PointLedgerRes(
        UUID id,
        UUID customerId,
        int delta,
        String reason,
        Instant createdAt
) {
    public static PointLedgerRes from(PointLedger entity) {
        return new PointLedgerRes(
                entity.getId(),
                entity.getCustomerId(),
                entity.getDelta(),
                entity.getReason(),
                entity.getCreatedAt()
        );
    }
}
