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

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", length = 30, nullable = false)
    private ServiceType serviceType;

    @Lob
    private String afterService;

    @Column(name = "duration_min", nullable = false)
    private int durationMin;

    @Column(name = "service_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal servicePrice = BigDecimal.ZERO;

    @Column(name = "designer_tip_percent", precision = 5, scale = 2)
    private BigDecimal designerTipPercent;

}
