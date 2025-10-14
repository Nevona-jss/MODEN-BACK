package com.moden.modenapi.modules.auth.service;

import com.moden.modenapi.modules.auth.dto.UserUpdateReq;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * ✅ UserService (UUID-based)
 * Handles profile updates (except immutable fields: name, phone).
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final HttpServletRequest request;

    /**
     * 🔹 Update user profile — name & phone are immutable.
     */
    public User updateProfile(UserUpdateReq req) {
        User user = getAuthenticatedUser();

        // 🔹 name & phone are intentionally NOT updatable
        // user.setName(...);
        // user.setPhone(...);

        if (req.email() != null ) {
            user.setEmail(req.email());
        }
        if (req.birthdate() != null) {
            user.setBirthdate(req.birthdate());
        }
        if (req.gender() != null) {
            user.setGender(req.gender());
        }
        if (req.consentMarketing() != null) {
            user.setConsentMarketing(req.consentMarketing());
        }
        if (req.address() != null ) {
            user.setAddress(req.address());
        }

        return userRepository.save(user);
    }

    /**
     * 🔹 Get current authenticated user's profile
     */
    public User getCurrentUser() {
        return getAuthenticatedUser();
    }

    // ══════════════════════════════════════════════════════
    // 🔒 Helper: Extract and validate authenticated user
    // ══════════════════════════════════════════════════════
    private User getAuthenticatedUser() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        if (!jwtProvider.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        String userIdStr = jwtProvider.getUserId(token);
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user ID format in token");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
