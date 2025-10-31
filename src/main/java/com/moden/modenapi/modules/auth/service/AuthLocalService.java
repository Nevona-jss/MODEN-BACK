package com.moden.modenapi.modules.auth.service;

import com.moden.modenapi.modules.auth.model.AuthLocal;
import com.moden.modenapi.modules.auth.repository.AuthLocalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthLocalService {

    private final AuthLocalRepository authLocalRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthLocal createOrUpdatePassword(UUID userId, String rawPassword) {
        String hash = passwordEncoder.encode(rawPassword);

        AuthLocal auth = authLocalRepository.findByUserId(userId)
                .orElseGet(() -> AuthLocal.builder().userId(userId).build());

        auth.setPasswordHash(hash);
        auth.setPasswordUpdatedAt(Instant.now());
        auth.setFailedAttempts(0);
        auth.setLockedUntil(null);
        auth.setForceReset(false);

        return authLocalRepository.save(auth);
    }

    @Transactional
    public boolean verifyPassword(UUID userId, String rawPassword) {
        AuthLocal auth = authLocalRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Local auth not found for userId: " + userId));

        if (auth.getLockedUntil() != null && auth.getLockedUntil().isAfter(Instant.now()))
            return false;

        boolean ok = passwordEncoder.matches(rawPassword, auth.getPasswordHash());
        if (ok) {
            auth.setFailedAttempts(0);
            auth.setLockedUntil(null);
            authLocalRepository.save(auth);
            return true;
        } else {
            int failed = auth.getFailedAttempts() + 1;
            auth.setFailedAttempts(failed);

            if (failed >= 5) {
                auth.setLockedUntil(Instant.now().plusSeconds(10 * 60));
                auth.setFailedAttempts(0);
            }

            authLocalRepository.save(auth);
            return false;
        }
    }

    @Transactional
    public void onFailedLogin(UUID userId) {
        AuthLocal auth = authLocalRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Local auth not found for userId: " + userId));

        if (auth.getLockedUntil() != null && auth.getLockedUntil().isAfter(Instant.now())) return;

        int failed = auth.getFailedAttempts() + 1;
        auth.setFailedAttempts(failed);
        if (failed >= 5) {
            auth.setLockedUntil(Instant.now().plusSeconds(10 * 60));
            auth.setFailedAttempts(0);
        }
        authLocalRepository.save(auth);
    }

    @Transactional
    public void resetFailed(UUID userId) {
        AuthLocal auth = authLocalRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Local auth not found for userId: " + userId));

        auth.setFailedAttempts(0);
        auth.setLockedUntil(null);
        authLocalRepository.save(auth);
    }

    @Transactional
    public void requirePasswordReset(UUID userId) {
        AuthLocal auth = authLocalRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Local auth not found for userId: " + userId));

        auth.setForceReset(true);
        authLocalRepository.save(auth);
    }

    @Transactional(readOnly = true)
    public boolean isLocked(UUID userId) {
        return authLocalRepository.findByUserId(userId)
                .map(a -> a.getLockedUntil() != null && a.getLockedUntil().isAfter(Instant.now()))
                .orElse(false);
    }
}
