package com.moden.modenapi.modules.auth.service;

import com.moden.modenapi.common.enums.DesignerStatus;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.modules.auth.dto.LoginReqStudioAndDesigner;
import com.moden.modenapi.modules.auth.dto.LoginResStudioAndDesigner;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.AuthLocalRepository;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import com.moden.modenapi.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IdLoginService {

    private final HairStudioDetailRepository studioRepo;
    private final DesignerDetailRepository designerRepo;
    private final UserRepository userRepo;
    private final AuthLocalRepository authLocalRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * Studio yoki Designer idForLogin orqali kirish.
     * - Parol tekshiriladi
     * - (ADMIN bo‘lmasa) kerakli rolega ko‘tariladi
     * - Access/Refresh token beriladi
     */
    public LoginResStudioAndDesigner login(LoginReqStudioAndDesigner req) {
        final String key = req.idForLogin() == null ? "" : req.idForLogin().trim();

        // 1) Studio orqali
        var studioOpt = studioRepo.findByIdForLoginIgnoreCase(key);
        if (studioOpt.isPresent()) {
            var studio = studioOpt.get();

            var user = userRepo.findById(studio.getUserId())
                    .orElseThrow(() -> unauthorized("User not found"));

            // Role ni HAIR_STUDIO ga ko‘tarish (ADMIN bo‘lsa o‘zgartirmaymiz)
            elevateRoleIfNeeded(user, Role.HAIR_STUDIO);

            // Parol tekshirish
            verifyPassword(user.getId(), req.password());

            // Tokenlar berish va javobni yig‘ish (DRY)
            return issueTokensAndBuildResponse(
                    user,
                    Role.HAIR_STUDIO,
                    studio.getId(),
                    studio.getIdForLogin()
            );
        }

        // 2) Designer orqali
        var designerOpt = designerRepo.findByIdForLoginIgnoreCase(key);
        if (designerOpt.isPresent()) {
            var designer = designerOpt.get();

            // (ixtiyoriy siyosat) faqat WORKING bo‘lgan dizaynerga ruxsat
            if (designer.getStatus() != DesignerStatus.WORKING) {
                throw unauthorized("Designer not active");
            }

            var user = userRepo.findById(designer.getUserId())
                    .orElseThrow(() -> unauthorized("User not found"));

            elevateRoleIfNeeded(user, Role.DESIGNER);

            verifyPassword(user.getId(), req.password());

            return issueTokensAndBuildResponse(
                    user,
                    Role.DESIGNER,
                    designer.getId(),
                    designer.getIdForLogin()
            );
        }

        // 3) topilmadi
        throw unauthorized("idForLogin not found");
    }

    /**
     * ADMIN bo‘lmasa, foydalanuvchi roli kerakli target’ga ko‘tariladi.
     */
    @Transactional
    void elevateRoleIfNeeded(User user, Role target) {
        if (user.getRole() == Role.ADMIN) return; // ADMIN ni o'zgartirmaymiz
        if (user.getRole() != target) {
            user.setRole(target);
            userRepo.save(user);
        }
    }

    /**
     * Foydalanuvchi parolini tekshirish.
     */
    void verifyPassword(UUID userId, String raw) {
        var auth = authLocalRepo.findByUserId(userId)
                .orElseThrow(() -> unauthorized("Password not set"));

        if (raw == null || raw.isBlank() || !passwordEncoder.matches(raw, auth.getPasswordHash())) {
            throw unauthorized("Invalid credentials");
        }
    }

    /**
     * DRY: Umumiy token berish va javobni yig‘ish.
     */
    private LoginResStudioAndDesigner issueTokensAndBuildResponse(
            User user,
            Role role,
            UUID targetId,       // studioId yoki designerId
            String idForLogin    // ko‘rsatilgan login id
    ) {
        String userIdStr = user.getId().toString();
        String at = jwtProvider.generateAccessToken(userIdStr, role.name());
        String rt = jwtProvider.generateRefreshToken(userIdStr);

        return new LoginResStudioAndDesigner(
                at,
                rt,
                role.name(),
                user.getId(),
                targetId,
                idForLogin
        );
        // Eslatma:
        // Controller RT ni Cookiega yozib, klient body’siga faqat AT yuborsa — security jihatdan eng to‘g‘ri yondashuv.
    }

    private ResponseStatusException unauthorized(String msg) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, msg);
    }
}
