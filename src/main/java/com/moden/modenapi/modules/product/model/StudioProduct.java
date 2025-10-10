package com.moden.modenapi.modules.product.model;

import java.math.BigDecimal;
import java.util.UUID;
import com.moden.modenapi.modules.studio.model.HairStudio;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

/**
 * Represents a product (e.g., hair care item) sold by a specific hair studio.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "studio_product")
public class StudioProduct {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uniqueidentifier")
    private UUID id;   // ✅ UUID instead of Long

    @ManyToOne(optional = false)
    @JoinColumn(name = "hair_studio_id")
    private HairStudio studio;

    @Column(nullable = false, length = 150)
    private String name;

    private String field;  // e.g. Haircare, Styling
    private String type;   // e.g. Shampoo, Conditioner

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Builder.Default
    private int stock = 0; // ✅ for quantity management
}
