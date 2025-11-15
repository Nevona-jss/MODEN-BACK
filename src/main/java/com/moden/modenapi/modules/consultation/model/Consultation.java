package com.moden.modenapi.modules.consultation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.enums.PaymentStatus;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "consultation")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Consultation extends BaseEntity {

    @Column(name = "reservation_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID reservationId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ConsultationStatus status = ConsultationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // ✅ 원하는 스타일 이미지 (고객이 요청한 스타일)
    @Column(name = "style_image_url", length = 500)
    private String wantedImageUrl;

    // ✅ 시술 전 이미지
    @Column(name = "before_image_url", length = 500)
    private String beforeImageUrl;

    // ✅ 시술 후 이미지
    @Column(name = "after_image_url", length = 500)
    private String afterImageUrl;

    @Lob
    private String consultationMemo;       // 상담 메모

    @Lob
    private String customerMemo;         // 고객 메모

    @Column(length = 500)
    private String drawingImageUrl;       // 그림 메모 (SVG/PNG URL)

}
