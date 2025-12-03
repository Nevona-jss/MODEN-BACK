package com.moden.modenapi.modules.auth.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CookieUtil;
import com.moden.modenapi.modules.auth.dto.*;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.auth.service.AuthLocalService;
import com.moden.modenapi.modules.auth.service.AuthMeService;
import com.moden.modenapi.modules.auth.service.AuthService;
import com.moden.modenapi.modules.auth.service.IdLoginService;
import com.moden.modenapi.modules.customer.dto.CustomerSignInRequest;
import com.moden.modenapi.security.JwtProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Tag(name = "AUTHENTICATION")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final IdLoginService idLoginService;
    private final AuthMeService authMeService;
    private final UserRepository userRepo;
    private final AuthLocalService authLocalService;

    // =========================
    // In-memory resetToken store (TTL 10 min)
    // =========================
    private static final Map<String, ResetToken> TOKENS = new ConcurrentHashMap<>();

    static class ResetToken {
        final UUID userId;
        final Instant expiresAt;
        ResetToken(UUID userId, Instant expiresAt) {
            this.userId = userId;
            this.expiresAt = expiresAt;
        }
    }

    // DTO/record lar (Phone verify/reset uchun)
    public record VerifyReq(@NotBlank String idToken) {}
    public record VerifyRes(String resetToken) {}
    public record ResetReq(@NotBlank String resetToken, @NotBlank String newPassword) {}

    // =========================
    // 기존 /me
    // =========================
    @GetMapping("/me")
    public ResponseEntity<ResponseMessage<?>> me() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String principal = (auth.getPrincipal() instanceof String s) ? s : auth.getName();
        UUID userId = UUID.fromString(principal);

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();

        Object payload = authMeService.getMePayload(userId, roles);
        return ResponseEntity.ok(ResponseMessage.success("OK", payload));
    }

    // =========================
    // Studio/Designer login by idForLogin
    // =========================
    @Operation(summary = "Studio/Designer login by idForLogin + password (refresh in cookie)")
    @PostMapping("/login-id")
    public ResponseEntity<ResponseMessage<Map<String, Object>>> signInById(
            @Valid @RequestBody LoginReqStudioAndDesigner req,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        LoginResStudioAndDesigner out = idLoginService.login(req);

        // ✅ RT → HttpOnly Cookie
        CookieUtil.setRefreshTokenCookie(response, request, out.refreshToken());

        Map<String, Object> body = Map.of(
                "accessToken", out.accessToken(),
                "refreshToken", out.refreshToken()
        );
        return ResponseEntity.ok(ResponseMessage.success("Login successful", body));
    }

    // =========================
    // Customer login (name + phone)
    // =========================
    @PostMapping("/login")
    public ResponseEntity<ResponseMessage<Map<String, String>>> signIn(
            @RequestBody CustomerSignInRequest req,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var tokens = authService.signInByNameAndPhone(req);

        CookieUtil.setRefreshTokenCookie(response, request, tokens.refreshToken());

        Map<String, String> data = Map.of(
                "accessToken", tokens.accessToken(),
                "refreshToken", tokens.refreshToken()
        );

        return ResponseEntity.ok(ResponseMessage.success("Login successful", data));
    }

    // =========================
    // Refresh
    // =========================
    @GetMapping("/refresh")
    public ResponseEntity<ResponseMessage<Map<String, String>>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = Arrays.stream(
                        Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseMessage.failure("Invalid or missing refresh token"));
        }

        if (!jwtProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseMessage.failure("Invalid or missing refresh token"));
        }

        String userId = jwtProvider.getUserId(refreshToken);
        String role   = jwtProvider.getUserRole(refreshToken);
        String newAccessToken = jwtProvider.generateAccessToken(userId, role);

        boolean secure = request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        String sameSite = secure ? "None" : "Lax";

        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ofDays(30))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        Map<String, String> data = Map.of("accessToken", newAccessToken);
        return ResponseEntity.ok(ResponseMessage.success("Token refreshed successfully", data));
    }

    // =========================
    // Logout
    // =========================
    @Operation(summary = "Logout (any role)", description = "Revoke current session, or all sessions with ?all=true")
    @PostMapping("/logout")
    public ResponseEntity<ResponseMessage<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(name = "all", defaultValue = "false") boolean revokeAll
    ) {
        authService.logout(request, response, revokeAll);
        return ResponseEntity.ok(ResponseMessage.success("Successfully logged out", null));
    }

    // ======================================================
    // 🔽 1) Studio register with phone (Firebase + extra fields)
    // ======================================================
    @Operation(summary = "Register new studio with verified phone")
    @PostMapping("/register")
    public ResponseEntity<ResponseMessage<StudioCreatedResponse>> registerStudio(
            @RequestBody @Valid RegisterStudioReq req
    ) {
        StudioCreatedResponse res = authService.registerStudio(req);

        return ResponseEntity.ok(
                ResponseMessage.success("Register success", res)
        );
    }




    // ======================================================
    // 🔽 3) resetToken bilan password reset
    // ======================================================
    @Operation(summary = "Reset password using resetToken")
    @PostMapping("/phone/reset-password")
    public ResponseEntity<ResponseMessage<String>> resetPassword(@RequestBody @Valid ResetReq req) {
        ResetToken rt = TOKENS.remove(req.resetToken());
        if (rt == null || rt.expiresAt.isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired resetToken");
        }

        authLocalService.createOrUpdatePassword(rt.userId, req.newPassword());
        return ResponseEntity.ok(ResponseMessage.success("Password reset success", "OK"));
    }
}
