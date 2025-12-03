package com.moden.modenapi.modules.reservation.model;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "reservation")
public class Reservation extends BaseEntity {

    @Column(name = "studio_id", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID studioId;

    @Column(name = "customer_id", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID customerId;

    @Column(name = "designer_id", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID designerId;

    @Column(name = "service_id", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID serviceId;

    @Column(name = "description", length = 1000)
    private String description;

    // 예약 날짜 (yyyy-MM-dd)
    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    // 시작 시간 (예: "08:00")
    @Column(name = "start_time", nullable = false, length = 5)
    private String startTime;

    // 종료 시간 (예: "09:15")
    @Column(name = "end_time", nullable = false, length = 5)
    private String endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ReservationStatus status;

}
