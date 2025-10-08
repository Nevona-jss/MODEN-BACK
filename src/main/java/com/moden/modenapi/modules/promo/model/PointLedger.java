package com.moden.modenapi.modules.promo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.serviceitem.model.StudioServiceItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name="point")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PointLedger {
    @Id @GeneratedValue @UuidGenerator
    @Column(columnDefinition="uniqueidentifier")
    private UUID id;

    @ManyToOne(optional=false)
    @JoinColumn(name="user_id")
    @JsonIgnoreProperties({"reservationsAsCustomer", "designerDetail"})
    private User user;

    @ManyToOne(optional=true)
    @JoinColumn(name="service_id")
    @JsonIgnoreProperties({"studio"})
    private StudioServiceItem service;

    @Column(nullable=false)
    private int amount;
    private String memo;

    @Builder.Default
    @Column(nullable=false)
    private Instant createdAt = Instant.now();
}
