package com.moden.modenapi.modules.auth.repository;

import com.moden.modenapi.modules.auth.model.AuthLocal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthLocalRepository extends JpaRepository<AuthLocal, UUID> {
    Optional<AuthLocal> findByUserId(UUID userId);

    // Atomic increment failed_attempts (native SQL). Returns number of rows updated (1 or 0).
    @Modifying
    @Query(value = "UPDATE auth_local SET failed_attempts = failed_attempts + 1 WHERE user_id = :userId", nativeQuery = true)
    int incrementFailedAttempts(@Param("userId") UUID userId);

    // Lock account: set failed_attempts = 0 and locked_until = :until
    @Modifying
    @Query(value = "UPDATE auth_local SET failed_attempts = 0, locked_until = :until WHERE user_id = :userId", nativeQuery = true)
    int lockAccount(@Param("userId") UUID userId, @Param("until") Instant until);

    // Reset failed attempts + clear lock
    @Modifying
    @Query(value = "UPDATE auth_local SET failed_attempts = 0, locked_until = NULL WHERE user_id = :userId", nativeQuery = true)
    int resetFailedAndUnlock(@Param("userId") UUID userId);
}
