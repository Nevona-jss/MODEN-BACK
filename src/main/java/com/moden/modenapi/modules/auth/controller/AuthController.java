package com.moden.modenapi.modules.auth.controller;

import com.moden.modenapi.common.enums.UserType;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.auth.dto.*;
import com.moden.modenapi.modules.auth.service.AuthService;
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
 * Controller for authentication and token lifecycle management.
 *
 * - Handles sign-up, sign-in (with name + phone)
 * - Issues Access token (to frontend) and Refresh token (in cookie)
 * - Supports /refresh endpoint for token renewal
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    // 🔹 CUSTOMER SIGN UP
    @PostMapping("/signup")
    public ResponseEntity<ResponseMessage<Void>> signUp(
            @RequestBody SignUpRequest req
    ) {
        // userType ni majburan CUSTOMER qilib qo'yamiz
        var fixedReq = new SignUpRequest(req.name(), req.phone(), UserType.CUSTOMER);
        authService.signUp(fixedReq);
        return ResponseEntity.ok(
                ResponseMessage.<Void>builder()
                        .success(true)
                        .message("Customer successfully registered.")
                        .data(null)
                        .build()
        );
    }


    @PostMapping("/signin")
    public ResponseEntity<ResponseMessage<Map<String, String>>> signIn(
            @RequestBody SignInRequest req,
            HttpServletResponse response
    ) {
        try {
            var tokens = authService.signInByNameAndPhone(req);

            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(Duration.ofDays(30))
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            Map<String, String> body = Map.of("accessToken", tokens.accessToken());

            return ResponseEntity.ok(
                    ResponseMessage.<Map<String, String>>builder()
                            .success(true)
                            .message("Login successful")
                            .data(body)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            // ❌ User not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseMessage.<Map<String, String>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .data(null)
                            .build());
        } catch (Exception e) {
            // ❌ Other errors (e.g., JWT generation)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseMessage.<Map<String, String>>builder()
                            .success(false)
                            .message("Login failed: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }


    // 🔹 REFRESH TOKEN
    @PostMapping("/refresh")
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
                    .body(ResponseMessage.<Map<String, String>>builder()
                            .success(false)
                            .message("Invalid or missing refresh token")
                            .data(null)
                            .build());
        }

        // Extract user info
        String userId = jwtProvider.getUserId(refreshToken);
        String role = jwtProvider.getUserRole(refreshToken);

        // Generate new tokens
        String newAccessToken = jwtProvider.generateAccessToken(userId, role);
        String newRefreshToken = jwtProvider.generateRefreshToken(userId);

        // 🍪 Replace refresh token cookie
        ResponseCookie newRefreshCookie = ResponseCookie.from("refresh_token", newRefreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofDays(30))
               // .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, newRefreshCookie.toString());

        // ✅ Return accessToken to frontend
        Map<String, String> tokens = Map.of("accessToken", newAccessToken);

        return ResponseEntity.ok(
                ResponseMessage.<Map<String, String>>builder()
                        .success(true)
                        .message("Token refreshed successfully")
                        .data(tokens)
                        .build()
        );
    }
}
