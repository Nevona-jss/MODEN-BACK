package com.moden.modenapi.modules.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(name="auth_local")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuthLocal {

    @Id
    @Column(columnDefinition="uniqueidentifier")
    private UUID userId;

    @OneToOne(fetch=FetchType.LAZY)
    @MapsId
    @JoinColumn(name="user_id")
    @JsonIgnoreProperties({"authLocal", "designerDetail"})
    private User user;

    @Column(nullable=false, length=255)
    private String passwordHash;

    @Builder.Default
    @Column(nullable=false)
    private Instant passwordUpdatedAt = Instant.now();

    @Builder.Default
    @Column(nullable=false)
    private int failedAttempts = 0;

    private Instant lockedUntil;

    @Builder.Default
    @Column(nullable=false)
    private boolean forceReset = false;
}
