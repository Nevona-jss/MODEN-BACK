package com.moden.modenapi.modules.auth.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.auth.dto.*;
import com.moden.modenapi.modules.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/signup")
    public ResponseEntity<ResponseMessage<Void>> signUp(
            @RequestBody SignUpRequest req,
            @RequestParam(defaultValue = "customer") String apiType
    ) {
        service.signUp(req, apiType);
        return ResponseEntity.ok(
                ResponseMessage.<Void>builder()
                        .success(true)
                        .message("User successfully registered")
                        .data(null)
                        .build()
        );
    }

    @PostMapping("/signin")
    public ResponseEntity<ResponseMessage<AuthResponse>> signIn(
            @RequestBody SignInRequest req,
            HttpServletResponse response
    ) {
        var tokens = service.signIn(req);

        // 🔹 Access token HTTP-only cookie
        ResponseCookie accessCookie = ResponseCookie.from("access_token", tokens.accessToken())
                .httpOnly(true)
                .secure(false) // HTTPS bo‘lsa true qil
                .path("/")
                .maxAge(60 * 60) // 1 soat
                .sameSite("Strict")
                .build();

        // 🔹 Refresh token HTTP-only cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7 kun
                .sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        return ResponseEntity.ok(
                ResponseMessage.<AuthResponse>builder()
                        .success(true)
                        .message("Login successful")
                        .data(tokens)
                        .build()
        );
    }
}
