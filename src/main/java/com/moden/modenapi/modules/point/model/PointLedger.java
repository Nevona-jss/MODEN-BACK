package com.moden.modenapi.modules.point.model;

import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single transaction in the user's point ledger.
 * Each record is either an addition (+) or deduction (-).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "point_ledger")
public class PointLedger extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uniqueidentifier")
    private UUID id;

    // ✅ Added missing field
    @Column(nullable = false)
    private UUID customerId;

    // ✅ Points added or deducted (positive = add, negative = subtract)
    @Column(nullable = false)
    private int delta;

    // ✅ Description of the reason for this transaction
    @Column(length = 255)
    private String reason;

    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
