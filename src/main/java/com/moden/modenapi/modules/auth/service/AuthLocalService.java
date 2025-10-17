package com.moden.modenapi.modules.auth.service;

import com.moden.modenapi.modules.auth.model.AuthLocal;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.AuthLocalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthLocalService {

    private final AuthLocalRepository authLocalRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${auth.maxFailedAttempts:5}")
    private int maxFailedAttempts;

    @Value("${auth.lockMinutes:15}")
    private long lockMinutes;

    @Transactional
    public void createOrUpdateLocalAuth(User user, String rawPassword, boolean forceReset) {
        if (rawPassword == null || rawPassword.isBlank()) throw new IllegalArgumentException("Password required");
        String hash = passwordEncoder.encode(rawPassword);

        AuthLocal auth = authLocalRepository.findByUserId(user.getId())
                .orElseGet(() -> AuthLocal.builder().user(user).userId(user.getId()).build());

        auth.setPasswordHash(hash);
        auth.setPasswordUpdatedAt(Instant.now());
        auth.setFailedAttempts(0);
        auth.setLockedUntil(null);
        auth.setForceReset(forceReset);

        authLocalRepository.save(auth);
    }

    public boolean matches(User user, String rawPassword) {
        return authLocalRepository.findByUserId(user.getId())
                .map(a -> passwordEncoder.matches(rawPassword, a.getPasswordHash()))
                .orElse(false);
    }

    @Transactional
    public void onFailedLogin(User user) {
        if (user == null || user.getId() == null) return;
        UUID uid = user.getId();

        Optional<AuthLocal> opt = authLocalRepository.findByUserId(uid);
        if (opt.isEmpty()) return;

        AuthLocal auth = opt.get();
        Instant now = Instant.now();

        // clear expired lock
        if (auth.getLockedUntil() != null && auth.getLockedUntil().isBefore(now)) {
            auth.setLockedUntil(null);
            auth.setFailedAttempts(0);
        }

        int attempts = auth.getFailedAttempts() + 1;

        if (attempts >= maxFailedAttempts) {
            Instant until = now.plus(lockMinutes, ChronoUnit.MINUTES);
            auth.setLockedUntil(until);
            auth.setFailedAttempts(0);
        } else {
            auth.setFailedAttempts(attempts);
        }

        authLocalRepository.save(auth);
    }

    @Transactional
    public void resetFailed(User user) {
        if (user == null || user.getId() == null) return;
        authLocalRepository.findByUserId(user.getId()).ifPresent(auth -> {
            auth.setFailedAttempts(0);
            auth.setLockedUntil(null);
            authLocalRepository.save(auth);
        });
    }

    public boolean isLocked(User user) {
        if (user == null || user.getId() == null) return false;
        return authLocalRepository.findByUserId(user.getId())
                .map(a -> a.getLockedUntil() != null && a.getLockedUntil().isAfter(Instant.now()))
                .orElse(false);
    }
}
