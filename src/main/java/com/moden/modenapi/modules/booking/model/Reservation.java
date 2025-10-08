package com.moden.modenapi.modules.booking.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.model.BaseEntity;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.serviceitem.model.StudioServiceItem;
import com.moden.modenapi.modules.studio.model.HairStudio;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "reservations",
        indexes = {
                @Index(name="ix_res_hs_time", columnList="hair_studio_id,reserved_at"),
                @Index(name="ix_res_des_time", columnList="designer_id,reserved_at"),
                @Index(name="ix_res_cust_time", columnList="customer_id,reserved_at")
        }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition="uniqueidentifier")
    private UUID id;

    @ManyToOne(optional=false)
    @JoinColumn(name="customer_id")
    @JsonIgnoreProperties({"reservations", "password", "roles"})
    private User customer;

    @ManyToOne(optional=false)
    @JoinColumn(name="designer_id")
    @JsonIgnoreProperties({"reservations", "password", "roles"})
    private User designer;

    @ManyToOne(optional=false)
    @JoinColumn(name="service_id")
    @JsonIgnoreProperties({"reservations"})
    private StudioServiceItem service;

    @ManyToOne(optional=false)
    @JoinColumn(name="hair_studio_id")
    @JsonIgnoreProperties({"reservations", "designers"})
    private HairStudio studio;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name="reserved_at", nullable=false)
    private Instant reservedAt;

    private String externalRef;
}
