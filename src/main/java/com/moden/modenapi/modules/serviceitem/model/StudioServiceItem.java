package com.moden.modenapi.modules.serviceitem.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.model.BaseEntity;
import com.moden.modenapi.modules.studio.model.HairStudio;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name="studio_service")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudioServiceItem extends BaseEntity {
    @Id @GeneratedValue @UuidGenerator
    @Column(columnDefinition="uniqueidentifier")
    private UUID id;

    @ManyToOne(optional=false)
    @JoinColumn(name="hair_studio_id")
    @JsonIgnoreProperties({"services", "reservations", "designers"})
    private HairStudio studio;

    @Column(nullable=false, length=150) private String name;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal price;
    @Column(nullable=false) private int durationMin;
    @Lob private String productUsed;
    @Lob private String description;
}
