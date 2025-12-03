package com.moden.modenapi.modules.coupon.model;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "coupon")
public class Coupon extends BaseEntity {

    @Column(name = "studio_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID studioId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "discount_rate")
    private BigDecimal discountRate;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Lob
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status = CouponStatus.AVAILABLE;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "used_date")
    private LocalDate usedDate;

}
