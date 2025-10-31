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
        name = "auth_local",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_auth_local_user_id", columnNames = "user_id")
        },
        indexes = {
                @Index(name = "ix_auth_local_user_id", columnList = "user_id")
        }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuthLocal extends BaseEntity {

    // 🔹 FK → users.id
    @Column(name = "user_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID userId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Builder.Default
    @Column(name = "password_updated_at", nullable = false)
    private Instant passwordUpdatedAt = Instant.now();

    @Builder.Default
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Builder.Default
    @Column(name = "force_reset", nullable = false)
    private boolean forceReset = false;
}
