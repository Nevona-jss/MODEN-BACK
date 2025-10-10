package com.moden.modenapi.modules.point.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(of = "id")
@Entity @Table(name="coupon")
public class Coupon {
    @Id @GeneratedValue @UuidGenerator
    @Column(columnDefinition="uniqueidentifier")
    private UUID id;

    @Column(nullable=false, length=150)
    private String name;
    private BigDecimal discount;  // absolute amount
    @Column(name = "[percent]")
    private BigDecimal percentValue;
    private LocalDate expiry;
}
