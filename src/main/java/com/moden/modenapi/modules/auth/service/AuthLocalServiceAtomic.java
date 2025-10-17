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
public class AuthLocalServiceAtomic {

    private final AuthLocalRepository repo;
    private final PasswordEncoder passwordEncoder;

    @Value("${auth.maxFailedAttempts:5}")
    private int maxFailedAttempts;

    @Value("${auth.lockMinutes:15}")
    private long lockMinutes;

    @Transactional
    public void onFailedLoginAtomic(User user) {
        if (user == null || user.getId() == null) return;
        UUID uid = user.getId();

        // atomically increment failed_attempts
        int updated = repo.incrementFailedAttempts(uid); // returns rows updated (1 or 0)
        if (updated == 0) return; // no record

        // reload current state
        Optional<AuthLocal> opt = repo.findByUserId(uid);
        if (opt.isEmpty()) return;
        AuthLocal auth = opt.get();

        // if counter reached threshold, lock via atomic update
        if (auth.getFailedAttempts() >= maxFailedAttempts) {
            Instant until = Instant.now().plus(lockMinutes, ChronoUnit.MINUTES);
            repo.lockAccount(uid, until); // sets failed_attempts = 0 and locked_until
        }
    }

    @Transactional
    public void resetFailedAtomic(User user) {
        if (user == null || user.getId() == null) return;
        repo.resetFailedAndUnlock(user.getId());
    }

    // create/update auth method similar to previous variant...
    @Transactional
    public void createOrUpdateLocalAuth(User user, String rawPassword, boolean forceReset) {
        if (rawPassword == null || rawPassword.isBlank()) throw new IllegalArgumentException("Password required");
        String hash = passwordEncoder.encode(rawPassword);

        Optional<AuthLocal> opt = repo.findByUserId(user.getId());
        if (opt.isPresent()) {
            AuthLocal a = opt.get();
            a.setPasswordHash(hash);
            a.setPasswordUpdatedAt(Instant.now());
            a.setFailedAttempts(0);
            a.setLockedUntil(null);
            a.setForceReset(forceReset);
            repo.save(a);
        } else {
            AuthLocal a = AuthLocal.builder()
                    .user(user)
                    .userId(user.getId())
                    .passwordHash(hash)
                    .passwordUpdatedAt(Instant.now())
                    .failedAttempts(0)
                    .forceReset(forceReset)
                    .build();
            repo.save(a);
        }
    }
}
