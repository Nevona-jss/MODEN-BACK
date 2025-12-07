package com.moden.modenapi.modules.payment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.enums.PaymentMethod;
import com.moden.modenapi.common.enums.PaymentStatus;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Payment extends BaseEntity {

    // 어떤 예약에 대한 결제인지
    @Column(name = "reservation_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID reservationId;

    // 어떤 쿠폰을 사용했는지 (고객 쿠폰 / 스튜디오 쿠폰 공통)
    @Column(name = "coupon_id", columnDefinition = "uniqueidentifier")
    private UUID couponId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20, nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod; // (CARD, CASH ...), 미결제면 null 허용

    // 서비스(시술) 총 금액
    @Column(name = "service_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal serviceTotal = BigDecimal.ZERO;

    @ElementCollection
    @CollectionTable(
            name = "payment_product_ids",
            joinColumns = @JoinColumn(
                    name = "payment_id",
                    columnDefinition = "uniqueidentifier"
            )
    )
    @Column(name = "product_id", columnDefinition = "uniqueidentifier")
    private List<UUID> productIds = new ArrayList<>();

    // 사용한 제품(샴푸, 트리트먼트 등) 총 금액
    @Column(name = "product_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal productTotal = BigDecimal.ZERO;

    // 포인트로 사용한 금액
    @Column(name = "points_used", precision = 12, scale = 2, nullable = false)
    private BigDecimal pointsUsed = BigDecimal.ZERO;


    // 실제로 고객이 지불해야 하는 최종 금액
    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    //  디자이너 인센티브 금액 (예: 3,000원)
    @Column(name = "designer_tip_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal designerTipAmount = BigDecimal.ZERO;

    @PrePersist
    void prePersist() {
        if (serviceTotal == null) serviceTotal = BigDecimal.ZERO;
        if (productTotal == null) productTotal = BigDecimal.ZERO;
        if (pointsUsed == null) pointsUsed = BigDecimal.ZERO;
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (designerTipAmount == null) designerTipAmount = BigDecimal.ZERO;
    }
}
