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

    public LoginResStudioAndDesigner login(LoginReqStudioAndDesigner req) {
        final String key = req.idForLogin().trim();

        // 1) Studio orqali
        var studioOpt = studioRepo.findByIdForLoginIgnoreCase(key);
        if (studioOpt.isPresent()) {
            var studio = studioOpt.get();
            var user = userRepo.findById(studio.getUserId())
                    .orElseThrow(() -> unauthorized("User not found"));
            // role-ni HAIR_STUDIO ga ko‘tarib qo‘yish (ADMIN bo‘lsa o‘zgartirmaymiz)
            elevateRoleIfNeeded(user, Role.HAIR_STUDIO);

            verifyPassword(user.getId(), req.password());
            var at = jwtProvider.generateAccessToken(user.getId().toString(), Role.HAIR_STUDIO.name());
            var rt = jwtProvider.generateRefreshToken(user.getId().toString());
            return new LoginResStudioAndDesigner(at, rt, Role.HAIR_STUDIO.name(), user.getId(), studio.getId(), studio.getIdForLogin());
        }

        // 2) Designer orqali
        var designerOpt = designerRepo.findByIdForLoginIgnoreCase(key);
        if (designerOpt.isPresent()) {
            var designer = designerOpt.get();

            // (ixtiyoriy) faqat ishlayotgan dizaynerga ruxsat
            if (designer.getStatus() != DesignerStatus.WORKING) {
                throw unauthorized("Designer not active");
            }

            var user = userRepo.findById(designer.getUserId())
                    .orElseThrow(() -> unauthorized("User not found"));
            elevateRoleIfNeeded(user, Role.DESIGNER);

            verifyPassword(user.getId(), req.password());
            var at = jwtProvider.generateAccessToken(user.getId().toString(), Role.DESIGNER.name());
            var rt = jwtProvider.generateRefreshToken(user.getId().toString());
            return new LoginResStudioAndDesigner(at, rt, Role.DESIGNER.name(), user.getId(), designer.getId(), designer.getIdForLogin());
        }

        // 3) topilmadi
        throw unauthorized("idForLogin not found");
    }

    @Transactional
    void elevateRoleIfNeeded(User user, Role target) {
        if (user.getRole() == Role.ADMIN) return; // adminni o'zgartirmaymiz
        if (user.getRole() != target) {
            user.setRole(target);
            userRepo.save(user);
        }
    }

    void verifyPassword(UUID userId, String raw) {
        var auth = authLocalRepo.findByUserId(userId)
                .orElseThrow(() -> unauthorized("Password not set"));
        if (raw == null || raw.isBlank() || !passwordEncoder.matches(raw, auth.getPasswordHash())) {
            throw unauthorized("Invalid credentials");
        }
    }

    private ResponseStatusException unauthorized(String msg) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, msg);
    }
}
