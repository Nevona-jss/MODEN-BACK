package com.moden.modenapi.modules.auth.service;

import com.moden.modenapi.modules.auth.model.UserSession;
import com.moden.modenapi.modules.auth.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;

    /** List only non-revoked sessions for a user */
    @Transactional(readOnly = true)
    public List<UserSession> getActiveSessions(UUID userId) {
        return userSessionRepository.findActiveByUserId(userId);
    }

    /** Revoke ALL sessions for a user (multi-device sign-out) */
    @Transactional
    public void revokeAllSessions(UUID userId) {
        // bulk update if you have it; otherwise loop
        List<UserSession> sessions = userSessionRepository.findByUserId(userId);
        for (UserSession s : sessions) {
            if (!s.isRevoked()) {
                s.setRevoked(true);
                s.setRevokedAt(OffsetDateTime.now());
                userSessionRepository.save(s);
            }
        }
    }

    /** Revoke a single session by userId + sessionId (preferred) */
    @Transactional
    public void revokeSession(UUID userId, String sessionId) {
        Optional<UserSession> os = userSessionRepository.findByUserIdAndSessionId(userId, sessionId);
        os.ifPresent(s -> {
            if (!s.isRevoked()) {
                s.setRevoked(true);
                s.setRevokedAt(OffsetDateTime.now());
                userSessionRepository.save(s);
            }
        });
    }

    /**
     * Revoke by access token (fallback when sid missing).
     * We never store raw tokens—only a hash.
     */
    @Transactional
    public void revokeByAccessToken(String rawAccessToken) {
        String hash = hashToken(rawAccessToken);
        Optional<UserSession> os = userSessionRepository.findByAccessTokenHash(hash);
        os.ifPresent(s -> {
            if (!s.isRevoked()) {
                s.setRevoked(true);
                s.setRevokedAt(OffsetDateTime.now());
                userSessionRepository.save(s);
            }
        });
        // If not found, you can optionally push the hash to a Redis blacklist for the remaining TTL.
    }

    /**
     * Create/Upsert session at login.
     * Call this from your login flow after issuing tokens.
     */
    @Transactional
    public UserSession upsertSessionOnLogin(
            UUID userId,
            String sessionId,            // the 'sid' you embed in JWTs
            String rawAccessToken,       // will be hashed here
            String refreshTokenId,       // jti of refresh token (recommended)
            String ip,
            String userAgent,
            OffsetDateTime accessExpAt,  // optional
            OffsetDateTime refreshExpAt  // optional
    ) {
        String accessHash = hashToken(rawAccessToken);

        UserSession s = userSessionRepository
                .findByUserIdAndSessionId(userId, sessionId)
                .orElseGet(UserSession::new);

        s.setUserId(userId);
        s.setSessionId(sessionId);
        s.setAccessTokenHash(accessHash);
        s.setRefreshTokenId(refreshTokenId);
        s.setIp(ip);
        s.setUserAgent(userAgent);
        s.setRevoked(false);
        s.setLastUsedAt(OffsetDateTime.now());
        s.setAccessExpiresAt(accessExpAt);
        s.setRefreshExpiresAt(refreshExpAt);

        return userSessionRepository.save(s);
    }

    /** Mark activity / touch session (optional) */
    @Transactional
    public void touch(String sessionId) {
        userSessionRepository.findBySessionId(sessionId).ifPresent(s -> {
            s.setLastUsedAt(OffsetDateTime.now());
            userSessionRepository.save(s);
        });
    }

    // ---------- helpers ----------

    private String hashToken(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(dig);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash token", e);
        }
    }
}
