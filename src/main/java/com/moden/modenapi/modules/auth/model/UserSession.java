package com.moden.modenapi.modules.auth.model;

import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_sessions")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession  extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Your JWT 'sid' (session id) */
    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    /** Never store raw token — store hash */
    @Column(name = "access_token_hash", length = 255)
    private String accessTokenHash;

    /** If you issue refresh with JTI, store it here */
    @Column(name = "refresh_token_id", length = 100)
    private String refreshTokenId;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column(name = "access_expires_at")
    private OffsetDateTime accessExpiresAt;

    @Column(name = "refresh_expires_at")
    private OffsetDateTime refreshExpiresAt;


}
