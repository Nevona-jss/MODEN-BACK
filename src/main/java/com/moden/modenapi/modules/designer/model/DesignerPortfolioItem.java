package com.moden.modenapi.modules.designer.model;

import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "designer_portfolio")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignerPortfolioItem extends BaseEntity {

    // FK(designer_id) comes from @JoinColumn in DesignerDetail.portfolio
    // No back-reference needed (unidirectional), so no @ManyToOne field required

    @Column(name = "designer_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID designerId;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @Column(name = "caption", length = 255)
    private String caption;
}
