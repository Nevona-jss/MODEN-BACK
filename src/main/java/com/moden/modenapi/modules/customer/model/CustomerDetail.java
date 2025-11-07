package com.moden.modenapi.modules.customer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.enums.Gender;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_detail")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CustomerDetail extends BaseEntity {

    @Column(name = "user_id", columnDefinition = "uniqueidentifier", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "designer_id", columnDefinition = "uniqueidentifier")
    private UUID designerId;

    @Column(name = "studio_id", columnDefinition = "uniqueidentifier")
    private  UUID studioId;

    @Column(length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    private LocalDate birthdate;

    @Column(length = 255)
    private String address;

    @Column(length = 255)
    private String profileImageUrl;

    @Column(name = "visit_reason", length = 500)
    private String visitReason;

    @Column(name = "consent_marketing", nullable = false)
    private boolean consentMarketing = false;

    private boolean notificationEnabled = false;  //카카오 알림톡 (필수)동의/미동의
}
