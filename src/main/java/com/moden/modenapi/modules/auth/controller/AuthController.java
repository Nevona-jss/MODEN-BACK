package com.moden.modenapi.modules.auth.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CookieUtil;
import com.moden.modenapi.modules.admin.AdminService;
import com.moden.modenapi.modules.auth.dto.*;
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

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;

@Tag(name = "AUTHENTICATION")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final IdLoginService idLoginService;
    private final AuthMeService authMeService;


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

        // Body → faqat AT va meta (RT ni body’da yubormaymiz)
        Map<String, Object> body = Map.of(
                "accessToken", out.accessToken(),
                "refreshToken", out.refreshToken()
        );
        return ResponseEntity.ok(ResponseMessage.success("Login successful", body));
    }



    @PostMapping("/login")
    public ResponseEntity<ResponseMessage<Map<String, String>>> signIn(
            @RequestBody CustomerSignInRequest req,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var tokens = authService.signInByNameAndPhone(req);

        // Existing cookie setting
        CookieUtil.setRefreshTokenCookie(response, request, tokens.refreshToken());

        // TEMP: Return both tokens for testing
        Map<String, String> data = Map.of(
                "accessToken", tokens.accessToken(),
                "refreshToken", tokens.refreshToken()
        );

        return ResponseEntity.ok(ResponseMessage.success("Login successful", data));
    }


    @GetMapping("/refresh")
    public ResponseEntity<ResponseMessage<Map<String, String>>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 1) Cookie'dan refresh_token ni o'qing (null-safe)
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

        // 2) RT ni tekshirish (mavjud validatsiya)
        if (!jwtProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseMessage.failure("Invalid or missing refresh token"));
        }

        // 3) Claimlar → yangi AT
        String userId = jwtProvider.getUserId(refreshToken);
        String role   = jwtProvider.getUserRole(refreshToken); // RT ichida role bo'lmasa, null qaytishi mumkin
        String newAccessToken = jwtProvider.generateAccessToken(userId, role);

        // 4) Render/Proxy orqasida HTTPS aniqlash: isSecure() || X-Forwarded-Proto=https
        boolean secure = request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        String sameSite = secure ? "None" : "Lax"; // HTTP devda Lax bo'lmasa cookie yuborilmaydi

        // 5) RT cookie'ni qayta yozish (rotation qilmayapmiz, mavjud RT ni saqlab qo'yish)
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(secure)          // HTTPS bo'lsa true bo'ladi
                .sameSite(sameSite)      // HTTPS: None, HTTP(dev): Lax
                .path("/")
                .maxAge(Duration.ofDays(30))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 6) Body → faqat yangi AT
        Map<String, String> data = Map.of("accessToken", newAccessToken);
        return ResponseEntity.ok(ResponseMessage.success("Token refreshed successfully", data));
    }


    // ----------------------------------------------------------------------
    // Logout → delegate to service (supports ?all=true)
    // ----------------------------------------------------------------------
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
}
