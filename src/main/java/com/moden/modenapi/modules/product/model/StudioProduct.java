package com.moden.modenapi.modules.product.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "studio_product")
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class StudioProduct extends BaseEntity {

    @Column(name = "studio_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID studioId;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "price", precision = 10, scale = 0, nullable = false)
    private BigDecimal price;

    /** 비고(제조사/용량 등) */
    @Column(name = "notes", length = 500)
    private String notes;

    /** hajm (litr) — 0.5, 1.0 ... */
    @Column(name = "volume_liters", precision = 7, scale = 2)
    private BigDecimal volumeLiters;

    /** dizayner tip (%) — 10.00 */
    @Column(name = "designer_tip_percent", precision = 5, scale = 2)
    private BigDecimal designerTipPercent;

    @Transient
    public BigDecimal getDesignerTipAmount() {
        if (price == null || designerTipPercent == null) return BigDecimal.ZERO;
        return price.multiply(designerTipPercent).divide(BigDecimal.valueOf(100));
    }

    @PrePersist
    void prePersist() {
        if (designerTipPercent == null) designerTipPercent = BigDecimal.ZERO;
    }
}
