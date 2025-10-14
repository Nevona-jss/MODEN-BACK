package com.moden.modenapi.modules.studio.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * ✅ HairStudioDetail
 * Extended salon information linked one-to-one with HairStudio.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "hair_studio_detail")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HairStudioDetail  extends BaseEntity {

    @Id
    @Column(name = "hair_studio_id", columnDefinition = "uniqueidentifier")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // share same PK as HairStudio
    @JoinColumn(name = "hair_studio_id")
    @JsonIgnoreProperties({"detail"}) // prevent infinite recursion
    private HairStudio studio;

    // 🔹 Optional descriptive fields
    @Lob
    private String description; // long-form intro text

    @Column(length = 200)
    private String parkingInfo; // e.g., “Free parking available”

    @Column(length = 255)
    private String naverUrl; // e.g., Naver Place link

    @Column(length = 255)
    private String blogUrl; // e.g., Naver Blog link

    @Column(length = 255)
    private String instagramUrl; // e.g., salon Instagram link

    @Column(columnDefinition = "nvarchar(max)")
    private String openHoursJson; // JSON-encoded open hours
}
