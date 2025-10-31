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
@Table(name = "user_sessions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UserSession extends BaseEntity {

    @Column(name = "user_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID userId;

    @Lob
    private String deviceInfo; // OS, model info

    @Column(length = 100)
    private String deviceId; // unique device ID

    @Column(length = 20)
    private String deviceType; // Android, iOS, Web

    @Column(length = 20)
    private String appVersion;

    @Column(length = 45)
    private String ipAddress;

    @Lob
    private String userAgent;

    @Column(length = 20)
    private String loginMethod; // PASSWORD, KAKAO, APPLE, etc.

    private boolean isAutoLogin;
    private boolean isPrimarySession;
    private boolean revoked;

    private Instant expiresAt;
    private Instant lastActivityAt;
}
