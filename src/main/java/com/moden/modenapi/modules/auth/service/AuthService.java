package com.moden.modenapi.modules.auth.service;

import com.moden.modenapi.common.enums.UserType;
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

    // 🔹 Ro‘yxatdan o‘tish (token bermaydi)
    public void signUp(SignUpRequest req, String apiType) {
        var existing = userRepository.findByPhone(req.phone()).orElse(null);
        if (existing == null) {
            UserType userType = UserType.CUSTOMER;
            if ("designer".equalsIgnoreCase(apiType)) {
                userType = UserType.DESIGNER;
            } else if ("salon".equalsIgnoreCase(apiType)) {
                userType = UserType.HAIR_STUDIO;
            }

            var user = User.builder()
                    .name(req.name())
                    .phone(req.phone())
                    .userType(userType)
                    .build();

            userRepository.save(user);
        }
    }

    // 🔹 Kirish — Access + Refresh token yaratish
    public AuthResponse signIn(SignInRequest req) {
        var user = userRepository.findByPhone(req.phone())
                .orElseThrow(() -> new RuntimeException("Foydalanuvchi topilmadi"));

        // access va refresh token yaratamiz
        String accessToken = jwtProvider.generateToken(user.getId().toString());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId().toString());

        return new AuthResponse(accessToken, refreshToken);
    }
}
