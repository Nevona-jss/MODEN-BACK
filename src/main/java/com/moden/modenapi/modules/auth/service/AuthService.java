package com.moden.modenapi.modules.auth.service;

import com.moden.modenapi.modules.auth.dto.*;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    public void signUp(SignUpRequest req) {
        userRepository.findByPhone(req.phone()).ifPresent(u -> {
            throw new IllegalArgumentException("User already registered with this phone number.");
        });

        var user = User.builder()
                .name(req.name())
                .phone(req.phone())
                .userType(req.userType())
                .build();

        userRepository.save(user);
    }

    public AuthResponse signIn(SignInRequest req) {
        var user = userRepository.findByPhone(req.phone())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtProvider.generateAccessToken(
                user.getId().toString(), user.getUserType().name());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId().toString());

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse signInByNameAndPhone(SignInRequest req) {
        var user = userRepository.findByNameAndPhone(req.name(), req.phone())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String accessToken = jwtProvider.generateAccessToken(
                user.getId().toString(), user.getUserType().name());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId().toString());

        return new AuthResponse(accessToken, refreshToken);
    }
}
