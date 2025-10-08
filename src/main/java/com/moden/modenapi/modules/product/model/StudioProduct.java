package com.moden.modenapi.modules.product.model;

import java.math.BigDecimal;

import com.moden.modenapi.modules.studio.model.HairStudio;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(of = "id")
@Entity @Table(name="studio_product")
public class StudioProduct {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false) @JoinColumn(name="hair_studio_id")
    private HairStudio studio;

    @Column(nullable=false, length=150) private String name;
    private String field;
    private String type;
    private BigDecimal price;
}
