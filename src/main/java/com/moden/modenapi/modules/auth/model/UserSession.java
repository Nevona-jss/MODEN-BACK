package com.moden.modenapi.modules.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="user_sessions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UserSession {

    @Id
    @Column(length=50)
    private String sessionId;

    @ManyToOne(optional=false)
    @JoinColumn(name="user_id")
    @JsonIgnoreProperties({"sessions", "designerDetail"}) // recursion to‘xtatish
    private User user;

    @Lob private String deviceInfo;
    @Column(length=100) private String deviceId;
    @Column(length=20) private String deviceType;
    @Column(length=20) private String appVersion;
    @Column(length=45) private String ipAddress;
    @Lob private String userAgent;
    @Column(length=20) private String loginMethod;

    private boolean isAutoLogin;
    private Integer concurrentSessionLimit;
    private boolean isPrimarySession;

    private Instant expiresAt;
    private Instant lastActivityAt;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private boolean revoked = false;
}
