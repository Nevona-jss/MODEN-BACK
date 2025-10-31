package com.moden.modenapi.modules.auth.service;

import java.util.UUID;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.modules.auth.dto.AuthResponse;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.customer.dto.CustomerSignInRequest;
import com.moden.modenapi.modules.customer.dto.CustomerSignUpRequest;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import com.moden.modenapi.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


/**
 * 🔹 AuthService
 * Handles user registration, login (fullName+phone / password), and token issuance.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthLocalService authLocalService;
    private final JwtProvider jwtProvider;
    private final HairStudioDetailRepository hairStudioDetailRepository;
    private final DesignerDetailRepository designerDetailRepository;
    private final CustomerDetailRepository customerDetailRepository;

    // ----------------------------------------------------------------------
    // 🔹 회원가입 (Sign Up)
    // ----------------------------------------------------------------------
    @Transactional
    public void signUp(CustomerSignUpRequest req, String rawPassword) {
        userRepository.findByPhone(req.phone()).ifPresent(u -> {
            throw new IllegalArgumentException("User already registered with this phone number.");
        });

        User user = User.builder()
                .fullName(req.fullName())
                .phone(req.phone())
                .build();

        userRepository.save(user);

        // password 저장 (optional)
        authLocalService.createOrUpdatePassword(user.getId(), rawPassword);
    }

    // ----------------------------------------------------------------------
    // 🔹 이름 + 전화번호 로그인 (Simple Login)
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public AuthResponse signInByNameAndPhone(CustomerSignInRequest req) {
        try {
            // 1️⃣ 사용자 조회
            User user = userRepository.findByFullNameAndPhone(req.fullName(), req.phone())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "User not found with given name and phone."
                    ));

            // 2️⃣ 역할 확인 (CUSTOMER / DESIGNER / STUDIO)
            Role role = resolveUserRole(user.getId());

            // 3️⃣ 토큰 생성
            String userIdStr = user.getId().toString();
            String accessToken = jwtProvider.generateAccessToken(userIdStr, role.name());
            String refreshToken = jwtProvider.generateRefreshToken(userIdStr);

            return new AuthResponse(accessToken, refreshToken);

        } catch (ResponseStatusException e) {
            // 이미 정의된 상태 예외는 그대로 전달
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unexpected error occurred during sign-in."
            );
        }
    }


    @Transactional
    public AuthResponse signInWithPassword(String phone, String fullName, String password) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean valid = authLocalService.verifyPassword(user.getId(), password);
        if (!valid) {
            authLocalService.onFailedLogin(user.getId());
            throw new IllegalArgumentException("Invalid password");
        }

        authLocalService.resetFailed(user.getId());

        // ✅ Dynamically resolve role (don’t call user.getRole())
        Role role = resolveUserRole(user.getId());

        String accessToken = jwtProvider.generateAccessToken(
                user.getId().toString(),
                role.name()
        );
        String refreshToken = jwtProvider.generateRefreshToken(user.getId().toString());

        return new AuthResponse(accessToken, refreshToken);
    }


    /**
     * ✅ Resolve the user's current role dynamically
     * based on which detail entity is present.
     */
    /**
     * ✅ Resolve the user's current role dynamically
     * based on which detail entity is present.
     */
    private Role resolveUserRole(UUID userId) {
        // Studio
        var studioOpt = hairStudioDetailRepository.findByUserId(userId);
        if (studioOpt.isPresent() && studioOpt.get().getRole() != null)
            return studioOpt.get().getRole();

        // Designer
        var designerOpt = designerDetailRepository.findByUserId(userId);
        if (designerOpt.isPresent() && designerOpt.get().getRole() != null)
            return designerOpt.get().getRole();

        // Customer
        var customerOpt = customerDetailRepository.findByUserId(userId);
        if (customerOpt.isPresent() && customerOpt.get().getRole() != null)
            return customerOpt.get().getRole();

        // fallback
        return Role.CUSTOMER;
    }




    // ----------------------------------------------------------------------
    // 🔹 비밀번호 로그인 (Password Login)
    // ----------------------------------------------------------------------
//    @Transactional
//    public AuthResponse signInWithPassword(String phone, String password) {
//        User user = userRepository.findByPhone(phone)
//                .orElseThrow(() -> new IllegalArgumentException("User not found"));
//
//        boolean valid = authLocalService.verifyPassword(user.getId(), password);
//        if (!valid) {
//            authLocalService.onFailedLogin(user.getId());
//            throw new IllegalArgumentException("Invalid password");
//        }
//
//        authLocalService.resetFailed(user.getId());
//
//        String accessToken = jwtProvider.generateAccessToken(
//                user.getId().toString().getRole().name());
//        String refreshToken = jwtProvider.generateRefreshToken(user.getId().toString());
//
//        return new AuthResponse(accessToken, refreshToken);
//    }
}
