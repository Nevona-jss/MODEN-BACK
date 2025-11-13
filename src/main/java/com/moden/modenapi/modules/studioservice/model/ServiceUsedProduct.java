package com.moden.modenapi.modules.studioservice.model;

import com.moden.modenapi.common.enums.MediaType;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a product used during a specific service.
 * Only stores productId and cost info (not full product object).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
@Entity
public class ServiceUsedProduct extends BaseEntity {

    // 🔹 Service ID (for traceability)
    @Column(name = "service_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID serviceId;


    @Column(name = "product_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID productId;  // FK → studio_product.id

    @Column(nullable = false)
    private int quantity;  // Miqdor

    @Column(precision = 12, scale = 7)
    private BigDecimal price;  // Mahsulot narxi

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice; // quantity × unitPrice
}
