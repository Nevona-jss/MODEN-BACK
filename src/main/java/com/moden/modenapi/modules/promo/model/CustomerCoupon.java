package com.moden.modenapi.modules.promo.model;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.modules.auth.model.User;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(of = "id")
@Entity
@Table(name="customer_coupon", indexes = @Index(name="ix_cc_user", columnList="customer_id"))
public class CustomerCoupon {
    @Id @GeneratedValue @UuidGenerator
    @Column(columnDefinition="uniqueidentifier")
    private UUID id;

    @ManyToOne(optional=false) @JoinColumn(name="customer_id")
    private User customer;

    @ManyToOne(optional=false) @JoinColumn(name="coupon_id")
    private Coupon coupon;

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private CouponStatus status = CouponStatus.ISSUED;

    @Builder.Default @Column(nullable=false)
    private Instant issuedAt = Instant.now();

    private Instant usedAt;
}
