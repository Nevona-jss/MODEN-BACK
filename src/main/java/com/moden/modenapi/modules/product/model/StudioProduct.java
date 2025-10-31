package com.moden.modenapi.modules.product.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a product (e.g., shampoo, color, or other item)
 * owned and managed by a specific hair studio.
 * Each salon registers and manages its own products.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "studio_product")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudioProduct extends BaseEntity {

    // 🔹 Belongs to which studio
    @Column(name = "studio_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID studioId; // FK → hair_studio_detail.id

    // 🔹 Product fullName (required)
    @Column(nullable = false, length = 150)
    private String name;

    // 🔹 Product category (e.g., Haircare, Styling)
    @Column(length = 100)
    private String category;

    // 🔹 Product type (e.g., Shampoo, Conditioner, Color)
    @Column(length = 100)
    private String type;

    // 🔹 Image URL or path (optional)
    @Column(name = "image", length = 500)
    private String image;

    // 🔹 Price of the product
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    // 🔹 Stock quantity (default 0)
    @Column(nullable = false)
    private int stock = 0;
}
