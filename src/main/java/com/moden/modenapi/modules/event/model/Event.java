package com.moden.modenapi.modules.event.model;

import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="event")
public class Event extends BaseEntity {

    @Column(name = "studio_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID studioId; // FK → hair_studio_detail.id

    @Column(nullable=false)
    private String title;

    @Lob
    private String description;

    @Column(name = "discount_amount")
    private BigDecimal discount;

    @Column(length = 500)
    private String imageUrl; // ✅ event banner image URL

    @Column(length = 255)
    private String instagramUrl; // Instagram link 자세히 보기기 버튼을 누르면

    private LocalDate startDate;
    private LocalDate endDate;
}
