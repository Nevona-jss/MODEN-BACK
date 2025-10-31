package com.moden.modenapi.modules.auth.service;

import com.moden.modenapi.modules.auth.model.UserSession;
import com.moden.modenapi.modules.auth.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSessionService {
    private final UserSessionRepository userSessionRepository;

    public List<UserSession> getActiveSessions(UUID userId) {
        return userSessionRepository.findActiveByUserId(userId);
    }

    public void revokeAllSessions(UUID userId) {
        userSessionRepository.findByUserId(userId).forEach(session -> {
            session.setRevoked(true);
            userSessionRepository.save(session);
        });
    }
}
