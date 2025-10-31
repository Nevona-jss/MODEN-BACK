package com.moden.modenapi.modules.studioservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.enums.ServiceType;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "studio_service")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudioService extends BaseEntity {

    @Column(name = "studio_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID studioId;

    @Column(name = "designer_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID designerId;

    @Column(name = "customer_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", length = 30, nullable = false)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", length = 30, nullable = false)
    private ReservationStatus reservationStatus = ReservationStatus.RESERVED;

    @Column(name = "reason_for_visiting", length = 255)
    private String reasonForVisiting;

    // ✅ 예약된 날짜 (예: 2025-10-20)
    @Column(name = "reserved_date", nullable = false)
    private LocalDate reservedDate;

    // ✅ 예약 시작 시각 (예: 2025-10-20T18:00)
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    // ✅ 예약 종료 시각 (예: 2025-10-20T19:00)
    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Lob
    private String description;

    @Column(name = "duration_min", nullable = false)
    private int durationMin;

    @Column(name = "service_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal servicePrice = BigDecimal.ZERO;
}
