package com.moden.modenapi.modules.auth.service;

import java.util.UUID;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.utils.DisplayNameUtil;
import com.moden.modenapi.modules.auth.dto.AuthResponse;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.dto.UserMeResponse;
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
    // Sign Up
    // ----------------------------------------------------------------------
    @Transactional
    public void signUp(CustomerSignUpRequest req, String rawPassword) {
        userRepository.findByPhone(req.phone()).ifPresent(u -> {
            throw new IllegalArgumentException("User already registered with this phone number.");
        });

        User user = User.builder()
                .fullName(req.fullName())
                .phone(req.phone())
                // optional: set default role at creation time
                .role(Role.CUSTOMER)
                .build();

        userRepository.save(user);

        // store password hash via local auth table
        authLocalService.createOrUpdatePassword(user.getId(), rawPassword);
    }

    // ----------------------------------------------------------------------
    // Name + Phone login
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public AuthResponse signInByNameAndPhone(CustomerSignInRequest req) {
        try {
            User user = userRepository.findByFullNameAndPhone(req.fullName(), req.phone())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "User not found with given name and phone."
                    ));

            Role role = resolveUserRole(user);

            String userIdStr = user.getId().toString();
            String accessToken = jwtProvider.generateAccessToken(userIdStr, role.name());
            String refreshToken = jwtProvider.generateRefreshToken(userIdStr);

            return new AuthResponse(accessToken, refreshToken);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unexpected error occurred during sign-in."
            );
        }
    }

    // ----------------------------------------------------------------------
    // Password login
    // ----------------------------------------------------------------------
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

        Role role = resolveUserRole(user);

        String accessToken = jwtProvider.generateAccessToken(
                user.getId().toString(),
                role.name()
        );
        String refreshToken = jwtProvider.generateRefreshToken(user.getId().toString());

        return new AuthResponse(accessToken, refreshToken);
    }

    // ----------------------------------------------------------------------
    // Role resolution
    // ----------------------------------------------------------------------
    /**
     * Decide the user's effective Role.
     * Priority:
     * 1) If User.role is set, use it.
     * 2) Else infer by existing detail entity:
     *    - has HairStudioDetail → STUDIO  (change to OWNER if your enum uses that)
     *    - else has DesignerDetail → DESIGNER
     *    - else → CUSTOMER
     */
    private Role resolveUserRole(User user) {
        if (user.getRole() != null) {
            return user.getRole();
        }

        UUID userId = user.getId();

        // studio?
        if (hairStudioDetailRepository.findByUserId(userId).isPresent()) {
            // map to your enum value for studio owner if different
            return Role.HAIR_STUDIO; // or Role.OWNER if that's your enum
        }

        // designer?
        if (designerDetailRepository.findByUserId(userId).isPresent()) {
            return Role.DESIGNER;
        }

        // customer?
        if (customerDetailRepository.findByUserId(userId).isPresent()) {
            return Role.CUSTOMER;
        }

        // fallback
        return Role.CUSTOMER;
    }

    //@Transactional(readOnly = true) // 선택
    public UserMeResponse getCurrentUserProfile(UUID userId, Role role) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UUID detailId = null;
        String detailName = null;

        if (role == null) role = Role.CUSTOMER; // 필요 시 기본값

        switch (role) {
            case CUSTOMER -> {
                var cd = customerDetailRepository.findByUserId(userId).orElse(null);
                if (cd != null) {
                    detailId = cd.getId();
                    detailName = DisplayNameUtil.extract(cd);
                }
            }
            case DESIGNER -> {
                var dd = designerDetailRepository.findByUserId(userId).orElse(null);
                if (dd != null) {
                    detailId = dd.getId();
                    detailName = DisplayNameUtil.extract(dd);
                }
            }
            case HAIR_STUDIO -> { // ← enum 이름 확인
                var sd = hairStudioDetailRepository.findByUserId(userId).orElse(null);
                if (sd != null) {
                    detailId = sd.getId();
                    detailName = DisplayNameUtil.extract(sd);
                }
            }
            case ADMIN -> {
                // detail 엔티티가 없다면 users 테이블에서 projection으로 표시명 확보
                var p = userRepository.findProfileByUserId(userId).orElse(null);
                if (p != null) {
                    detailId = p.getId();
                    detailName = p.getDisplayName();
                }
            }
            default -> { /* 다른 롤 생기면 여기서 처리 */ }
        }

        // 1차 실패 시: user.fullName
        if (detailName == null || detailName.isBlank()) {
            detailName = user.getFullName();
        }
        // 2차 안전망: 그래도 없으면 phone/id로라도
        if (detailName == null || detailName.isBlank()) {
            detailName = userRepository.findProfileByUserId(userId)
                    .map(p -> p.getDisplayName())
                    .orElse(String.valueOf(user.getId()));
        }

        return UserMeResponse.builder()
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
