package com.moden.modenapi.modules.studio.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.enums.Position;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "hair_studio_detail")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HairStudioDetail extends BaseEntity {

    @Column(name = "user_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID userId;

    @Column(name = "studio_code", unique = true, length = 50)
    private String idForLogin;

    @Column(nullable = false, length = 100)
    private String businessNo;

    @Column(nullable = false, length = 100)
    private String ownerName;

    @Column(length = 50)
    private String studioPhone;

    @Column(length = 255)
    private String address;

    @Column(length = 500)
    private String logoImageUrl;

    @Column(length = 500)
    private String bannerImageUrl;

    @Column(length = 255)
    private String profileImageUrl;

    @Lob
    private String description;

    @Column(length = 200)
    private String parkingInfo;

    @Column(length = 255)
    private String naverUrl;

    @Column(length = 255)
    private String kakaoUrl;

    @Column(length = 255)
    private String instagramUrl;

    @Enumerated(EnumType.STRING)
    private Position position = Position.STUDIO_OWNER;

    @Column(columnDefinition = "nvarchar(max)")
    private String openHoursJson;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;
}
