package com.moden.modenapi.modules.reservation.model;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "reservation")
public class Reservation extends BaseEntity {

    @Column(name = "customer_id", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID customerId;

    @Column(name = "designer_id", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID designerId;

    @Column(name = "service_id", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID serviceId;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "reservation_at", nullable = false)
    private LocalDateTime reservationAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ReservationStatus status;
}
