
package com.moden.modenapi.modules.coupon.model;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
        import lombok.*;

        import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "customer_coupon")
public class CustomerCoupon extends BaseEntity {

    @Column(name = "studio_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID studioId;

    @Column(name = "coupon_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID couponId;   // → Coupon.id (policy)

    @Column(name = "customer_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID customerId; // foydalanuvchi emas, biznes customer

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status = CouponStatus.AVAILABLE; // AVAILABLE → (redeem) → EXPIRED

    @Column(name = "issued_at")
    private Instant issuedAt; // qachon berilgan (ixtiyoriy, lekin foydali)

    @Column(name = "used_at")
    private Instant usedAt;;
}
