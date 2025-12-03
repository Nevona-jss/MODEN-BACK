package com.moden.modenapi.modules.auth.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.utils.IdGenerator;
import com.moden.modenapi.modules.auth.dto.AuthResponse;
import com.moden.modenapi.modules.auth.dto.LoginResStudioAndDesigner;
import com.moden.modenapi.modules.auth.dto.RegisterStudioReq;
import com.moden.modenapi.modules.auth.dto.StudioCreatedResponse;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.customer.dto.CustomerSignInRequest;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
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
    private final JwtProvider jwtProvider;
    private final HairStudioDetailRepository hairStudioDetailRepository;
    private final DesignerDetailRepository designerDetailRepository;
    private final CustomerDetailRepository customerDetailRepository;
    private final UserSessionService userSessionService;
    private final AuthLocalService  authLocalService;


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


    @Transactional
    public StudioCreatedResponse registerStudio(RegisterStudioReq req) {

        // 0) 로그인용 ID 생성 (예: ST-MONA-123456)
        String studioLoginId = IdGenerator.generateStudioId(req.shopName());

        // 1) User 생성
        User newUser = User.builder()
                .phone(req.ownerPhone())
                .phoneVerified(true)
                .phoneVerifiedAt(Instant.now())
                .role(Role.HAIR_STUDIO)
                .build();

        User user = userRepository.save(newUser);

        // 2) 비밀번호 저장
        authLocalService.createOrUpdatePassword(user.getId(), req.password());

        // 3) HairStudioDetail 저장
        HairStudioDetail studio = HairStudioDetail.builder()
                .userId(user.getId())
                .idForLogin(studioLoginId)   // ★ 생성한 로그인 ID 사용
                .businessNo(req.businessNo())
                .ownerName(req.ownerName())
                .build();
        hairStudioDetailRepository.save(studio);

        // 4) 응답 DTO 생성
        return new StudioCreatedResponse(
                user.getPhone(),      // ownerPhone
                req.shopName(),       // shopName (원래 입력한 상호)
                studioLoginId         // idForLogin (예: ST-MONA-123456)
        );
    }



}