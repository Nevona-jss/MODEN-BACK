package com.moden.modenapi.modules.studio.model;

import com.moden.modenapi.common.model.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * ✅ HairStudio
 * Base salon information (core business registration + contact info).
 * Connected one-to-one with HairStudioDetail for extended information.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "hair_studio")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HairStudio extends BaseEntity {

    // ✅ Primary key (UUID)
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uniqueidentifier")
    private UUID id;

    // ✅ Login ID (custom generated from name)
    @Column(name = "studio_code", unique = true, length = 50)
    private String idForLogin;

    // 🔹 Required fields
    @Column(nullable = false, length = 150)
    private String name; // Studio name

    @Column(nullable = false, length = 100)
    private String businessNo; // Business registration number

    @Column(nullable = false, length = 100)
    private String owner; // Owner name

    // 🔹 Optional fields
    @Column(length = 50)
    private String ownerPhone;

    @Column(length = 50)
    private String studioPhone;

    @Column(length = 255)
    private String address;

    @Column(length = 255)
    private String logo;

    @Column(length = 255)
    private String instagram;

    @Column(length = 255)
    private String naver;

    @Column(length = 255)
    private String qrCodeUrl;

    // 🔹 Relationship
    @OneToOne(mappedBy = "studio", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnoreProperties({"studio"})
    private HairStudioDetail detail;
}
