package com.moden.modenapi.modules.studio.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.enums.Position;
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

    private String ownerName;

    @Column(length = 50)
    private String studioPhone;

    @Column(length = 255)
    private String address;

    @Column(length = 500)
    private String logoImageUrl;

    @Column(length = 500)
    private String bannerImageUrl;

    @Lob
    private String description;

    @Column(length = 255)
    private String naverUrl;

    @Column(length = 255)
    private String kakaoUrl;

    @Enumerated(EnumType.STRING)
    private Position position = Position.STUDIO_OWNER;

    @Column(columnDefinition = "nvarchar(max)")
    private String openHoursJson;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "birthday_coupon_enabled", nullable = false)
    private boolean birthdayCouponEnabled = false;

    @Column(name = "birthday_coupon_description", columnDefinition = "nvarchar(max)")
    private String birthdayCouponDescription;

    /**
     * 3) 보안 및 개인정보 안내 (HTML formatda keladi, lekin String sifatida saqlanadi)
     *    HTML uzun bo‘lishi mumkin, shuning uchun nvarchar(max) + @Lob ishlatdik.
     */
    @Lob
    @Column(name = "privacy_policy_html", columnDefinition = "nvarchar(max)")
    private String privacyPolicyHtml;

}
