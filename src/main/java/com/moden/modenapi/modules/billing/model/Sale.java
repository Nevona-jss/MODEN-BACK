package com.moden.modenapi.modules.billing.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.modules.booking.model.Reservation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "sale")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Sale {
    @Id @GeneratedValue @UuidGenerator
    @Column(columnDefinition = "uniqueidentifier")
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "reservation_id")
    @JsonIgnoreProperties({"studio", "designer", "customer", "service"})
    private Reservation reservation;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
