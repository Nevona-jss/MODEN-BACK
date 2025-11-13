package com.moden.modenapi.modules.auth.service;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.utils.DisplayNameUtil;
import com.moden.modenapi.modules.auth.dto.AuthResponse;
import com.moden.modenapi.modules.auth.dto.UserMeFullResponse;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.customer.dto.CustomerSignInRequest;
import com.moden.modenapi.modules.customer.dto.CustomerSignUpRequest;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import com.moden.modenapi.security.JwtProvider;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthLocalService authLocalService;
    private final JwtProvider jwtProvider;
    private final HairStudioDetailRepository hairStudioDetailRepository;
    private final DesignerDetailRepository designerDetailRepository;
    private final CustomerDetailRepository customerDetailRepository;
    private final UserSessionService userSessionService;


    // ----------------------------------------------------------------------
    // Name + Phone login
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public AuthResponse signInByNameAndPhone(CustomerSignInRequest req) {
        try {
            User user = userRepository.findByFullNameAndPhone(req.fullName(), req.phone())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "User not found with given name and phone."));

            Role role = resolveUserRole(user);

            String userIdStr = user.getId().toString();
            String accessToken = jwtProvider.generateAccessToken(userIdStr, role.name());
            String refreshToken = jwtProvider.generateRefreshToken(userIdStr);

            return new AuthResponse(accessToken, refreshToken);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred during sign-in.");
        }
    }

    // ----------------------------------------------------------------------
    // Logout (moved to service)
    // ----------------------------------------------------------------------
    @Transactional
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       boolean revokeAll) {

        // 1) Resolve access token (Authorization header -> access_token cookie)
        String token = resolveAccessToken(request);
        if (token == null) {
            clearRefreshCookie(response);
            clearAccessCookieIfPresent(response);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }

        // 2) Validate token
        if (!jwtProvider.validateToken(token)) {
            clearRefreshCookie(response);
            clearAccessCookieIfPresent(response);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        // 3) Extract user/session
        UUID userId = UUID.fromString(jwtProvider.getUserId(token));
        Optional<String> sid = getSessionIdFromJwt(token); // "sid"/"sessionId" claim

        // 4) Revoke session(s)
        if (revokeAll) {
            userSessionService.revokeAllSessions(userId);
        } else {
            if (sid.isPresent()) {
                userSessionService.revokeSession(userId, sid.get());
            } else {
                userSessionService.revokeByAccessToken(token);
            }
        }

        // 5) Cookie hygiene
        clearRefreshCookie(response);
        clearAccessCookieIfPresent(response);
    }

    // ----------------- private helpers -----------------

    private String resolveAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return readCookie(request, "access_token").orElse(null);
    }

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]);
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie expired = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
    }

    private void clearAccessCookieIfPresent(HttpServletResponse response) {
        ResponseCookie expired = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
    }

    private Optional<String> getSessionIdFromJwt(String token) {
        try {
            // If you have a dedicated method:
            // return jwtProvider.getSessionId(token);
            String sid = (String) jwtProvider.getClaim(token, "sid"); // or "sessionId"
            return Optional.ofNullable(sid);
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

    // ----------------------------------------------------------------------
    // Role resolution
    // ----------------------------------------------------------------------
    private Role resolveUserRole(User user) {
        if (user.getRole() != null) return user.getRole();

        UUID userId = user.getId();

        if (hairStudioDetailRepository.findByUserIdAndDeletedAtIsNull(userId).isPresent()) {
            return Role.HAIR_STUDIO;
        }
        if (designerDetailRepository.findByUserIdAndDeletedAtIsNull(userId).isPresent()) {
            return Role.DESIGNER;
        }
        if (customerDetailRepository.findByUserId(userId).isPresent()) {
            return Role.CUSTOMER;
        }
        return Role.CUSTOMER;
    }

    public UserMeFullResponse getCurrentUserProfile(UUID userId, Role role) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UUID detailId = null;
        String detailName = null;

        if (role == null) role = Role.CUSTOMER;

        switch (role) {
            case CUSTOMER -> {
                var cd = customerDetailRepository.findByUserId(userId).orElse(null);
                if (cd != null) { detailId = cd.getId(); detailName = DisplayNameUtil.extract(cd); }
            }
            case DESIGNER -> {
                var dd = designerDetailRepository.findByUserIdAndDeletedAtIsNull(userId).orElse(null);
                if (dd != null) { detailId = dd.getId(); detailName = DisplayNameUtil.extract(dd); }
            }
            case HAIR_STUDIO -> {
                var sd = hairStudioDetailRepository.findByUserIdAndDeletedAtIsNull(userId).orElse(null);
                if (sd != null) { detailId = sd.getId(); detailName = DisplayNameUtil.extract(sd); }
            }
            case ADMIN -> {
                // ❗ findProfileByUserId() proektsiyasida getId() bo'lmagani uchun
                // vaqtincha foydalanuvchining ma'lumotlari bilan to'ldiramiz (yoki 5-banddagi projection’ni qo‘llang)
                detailId = user.getId();
                detailName = (user.getFullName() != null && !user.getFullName().isBlank())
                        ? user.getFullName() : String.valueOf(user.getId());
            }
            default -> {}
        }

        if (detailName == null || detailName.isBlank()) detailName = user.getFullName();
        if (detailName == null || detailName.isBlank()) detailName = String.valueOf(user.getId());

        return UserMeFullResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(role.name())
                .detailId(detailId)
                .detailName(detailName)
                .createdAt(user.getCreatedAt())
                .build();
    }
}