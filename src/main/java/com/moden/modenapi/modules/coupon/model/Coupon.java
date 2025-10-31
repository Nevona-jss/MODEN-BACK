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

    @Column(name = "user_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String name;   // e.g. "Birthday 10% Discount Coupon"

    @Column(name = "discount_rate", precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status = CouponStatus.AVAILABLE; // AVAILABLE, USED, EXPIRED

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "is_birthday_coupon")
    private boolean birthdayCoupon = false;

    @Column(name = "is_first_visit_coupon")
    private boolean firstVisitCoupon = false;
}
