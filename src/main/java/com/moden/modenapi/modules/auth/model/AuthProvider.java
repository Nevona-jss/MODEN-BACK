package com.moden.modenapi.modules.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "auth_provider",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_uid"})
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuthProvider  extends BaseEntity {

    // 🔹 Foreign Keys
    @Column(name = "user_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID userId;  // FK → users.id

    @Column(nullable = false, length = 30)
    private String provider; // KAKAO, APPLE, NAVER, etc.

    @Column(name = "provider_uid", nullable = false, length = 200)
    private String providerUid;

}
