package com.moden.modenapi.modules.point.model;

import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Defines cashback (point) policy for each studio.
 * Example: 5% → customer gets 5% of payment amount as points.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "studio_point_policy")
public class StudioPointPolicy extends BaseEntity {

    @Column(name = "studio_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID studioId; // FK → hair_studio.id

    @Column(name = "point_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal pointRate; // e.g., 5.00 → 5

}
