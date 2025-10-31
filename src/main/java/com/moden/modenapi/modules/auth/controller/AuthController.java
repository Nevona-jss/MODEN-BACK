package com.moden.modenapi.modules.auth.controller;

import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CookieUtil;
import com.moden.modenapi.modules.auth.service.AuthService;
import com.moden.modenapi.modules.auth.service.UserSessionService;
import com.moden.modenapi.modules.customer.dto.CustomerSignInRequest;
import com.moden.modenapi.modules.customer.dto.CustomerSignUpRequest;
import com.moden.modenapi.security.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;

/**
 * ✅ AuthController
 * Handles:
 *  - Customer Sign Up
 *  - Sign In
 *  - Refresh Token
 *  - Logout
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final UserSessionService userSessionService;

    // ----------------------------------------------------------------------
    // 🔹 SIGN UP (CUSTOMER)
    // ----------------------------------------------------------------------
    @PostMapping("/signup")
    public ResponseEntity<ResponseMessage<Void>> signUp(@RequestBody CustomerSignUpRequest req) {
        // Force CUSTOMER role for all signups
        CustomerSignUpRequest fixedReq =
                new CustomerSignUpRequest(req.fullName(), req.phone(), req.studioId(), Role.CUSTOMER);

        authService.signUp(fixedReq, "default123!");

        return ResponseEntity.ok(
                ResponseMessage.<Void>builder()
                        .success(true)
                        .message("Customer successfully registered.")
                        .build()
        );
    }



    // ----------------------------------------------------------------------
    // 🔹 SIGN IN (NAME + PHONE)
    // ----------------------------------------------------------------------
    @PostMapping("/signin")
    public ResponseEntity<ResponseMessage<Map<String, String>>> signIn(
            @RequestBody CustomerSignInRequest req,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            var tokens = authService.signInByNameAndPhone(req);

            // ✅ Store refresh token in secure cookie
            CookieUtil.setRefreshTokenCookie(response, request, tokens.refreshToken());

            Map<String, String> data = Map.of("accessToken", tokens.accessToken());
            return ResponseEntity.ok(ResponseMessage.success("Login successful", data));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseMessage.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseMessage.failure("Login failed: " + e.getMessage()));
        }
    }

    // ----------------------------------------------------------------------
    // 🔹 REFRESH TOKEN
    // ----------------------------------------------------------------------
    @GetMapping("/refresh")
    public ResponseEntity<ResponseMessage<Map<String, String>>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        if (refreshToken == null || !jwtProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseMessage.failure("Invalid or missing refresh token"));
        }

        String userId = jwtProvider.getUserId(refreshToken);
        String role = jwtProvider.getUserRole(refreshToken);

        String newAccessToken = jwtProvider.generateAccessToken(userId, role);

        // ✅ Update cookie (secure, consistent)
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofDays(30))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        Map<String, String> tokens = Map.of("accessToken", newAccessToken);
        return ResponseEntity.ok(ResponseMessage.success("Token refreshed successfully", tokens));
    }


    // ----------------------------------------------------------------------
    // 🔹 LOGOUT
    // ----------------------------------------------------------------------
    @PostMapping("/logout")
    public ResponseEntity<ResponseMessage<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseMessage.failure("Missing or invalid token"));
        }

        String token = authHeader.substring(7);
        if (!jwtProvider.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseMessage.failure("Invalid or expired token"));
        }

        UUID userId = UUID.fromString(jwtProvider.getUserId(token));
        userSessionService.revokeAllSessions(userId);

        // 🧹 Remove cookie on client side
        ResponseCookie expiredCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)          // ← keep secure=true for consistency with CookieUtil
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
        return ResponseEntity.ok(ResponseMessage.success("Successfully logged out", null));
    }
}
