package com.moden.modenapi.modules.payment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.enums.PaymentMethod;
import com.moden.modenapi.common.enums.PaymentStatus;
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
@Table(name = "payment")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Payment extends BaseEntity {

    // ✅ 연결된 서비스 ID
    @Column(name = "service_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID serviceId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod paymentMethod; // (CARD, CASH 등)

    // 🎟 쿠폰 할인
    @Column(name = "coupon_discount", precision = 12, scale = 2)
    private BigDecimal couponDiscount = BigDecimal.ZERO;

    // 💎 포인트 사용
    @Column(name = "points_used", precision = 12, scale = 2)
    private BigDecimal pointsUsed = BigDecimal.ZERO;

    // ✅ 최종 결제 금액 (계산된 금액)
    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;
}
