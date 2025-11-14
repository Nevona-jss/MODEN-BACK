package com.moden.modenapi.modules.point.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.enums.PointType;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "point")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Point extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title = "Studio Point";

    @Column(name = "user_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID userId;   // ✅ customer의 userId

    @Column(
            name = "payment_id",
            columnDefinition = "uniqueidentifier",
            nullable = true        // ✅ manual grant에서 null 허용
    )
    private UUID paymentId;


    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private PointType type;  // EARN / USE

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
}
