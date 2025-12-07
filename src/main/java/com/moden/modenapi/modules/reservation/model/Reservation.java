package com.moden.modenapi.modules.reservation.model;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "start_time", nullable = false, length = 5)
    private String startTime;

    @Column(name = "end_time", nullable = false, length = 5)
    private String endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ReservationStatus status;

    @ElementCollection
    @CollectionTable(
            name = "reservation_service_ids",
            joinColumns = @JoinColumn(name = "reservation_id", columnDefinition = "uniqueidentifier")
    )
    @Column(name = "service_id", columnDefinition = "uniqueidentifier")
    private List<UUID> serviceIds = new ArrayList<>();
}
