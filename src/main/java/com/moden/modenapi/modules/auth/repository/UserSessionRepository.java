package com.moden.modenapi.modules.auth.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.auth.model.UserSession;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends BaseRepository<UserSession, UUID> {

    List<UserSession> findByUserId(UUID userId);

    Optional<UserSession> findByUserIdAndSessionId(UUID userId, String sessionId);

    Optional<UserSession> findBySessionId(String sessionId);

    Optional<UserSession> findByAccessTokenHash(String accessTokenHash);

    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.revoked = false")
    List<UserSession> findActiveByUserId(@Param("userId") UUID userId);
}
