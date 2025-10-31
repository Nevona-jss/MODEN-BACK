package com.moden.modenapi.modules.designer.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.common.utils.IdGenerator;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.auth.service.AuthLocalService;
import com.moden.modenapi.modules.designer.dto.DesignerCreateDto;
import com.moden.modenapi.modules.designer.dto.DesignerResponse;
import com.moden.modenapi.modules.designer.model.DesignerDetail;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.moden.modenapi.common.enums.Role.DESIGNER;

@Service
@RequiredArgsConstructor
@Transactional
public class DesignerService extends BaseService<DesignerDetail> {

    private final DesignerDetailRepository designerRepository;
    private final UserRepository userRepository;
    private final AuthLocalService authLocalService;
    private final JwtProvider jwtProvider;
    private final HttpServletRequest request;

    @Override
    protected DesignerDetailRepository getRepository() {
        return designerRepository;
    }

    // ----------------------------------------------------------------------
    // 🔹 CREATE DESIGNER (by Studio)
    // ----------------------------------------------------------------------
    public DesignerResponse createDesigner(DesignerCreateDto req) {
        UUID studioId = req.hairStudioId();
        if (studioId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hairStudioId is required");
        }

        // 1️⃣ Generate or validate login code
        String loginCode = (req.idForLogin() == null || req.idForLogin().isBlank())
                ? generateUniqueDesignerCode()
                : req.idForLogin().trim();

        if (designerRepository.existsByIdForLogin(loginCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Designer login ID already exists: " + loginCode);
        }

        // 2️⃣ Create User with role DESIGNER
        User user = User.builder()
                .phone(req.phone() != null ? req.phone() : "UNKNOWN")
                .build();
        userRepository.save(user);
        // 3️⃣ Create DesignerDetail entity
        DesignerDetail designer = DesignerDetail.builder()
                .userId(user.getId())
                .bio(req.bio())
                .portfolioUrl(req.portfolioUrl())
                .hairStudioId(studioId)
                .role(DESIGNER)
                .position(req.position())
                .idForLogin(loginCode)
                .build();

        create(designer);

        // 4️⃣ Create password record
        authLocalService.createOrUpdatePassword(user.getId(), req.password());

        return mapToRes(designer);
    }

    // 🔹 Generate unique login code
    private String generateUniqueDesignerCode() {
        for (int i = 0; i < 5; i++) {
            String code = IdGenerator.generateId("DS");
            if (!designerRepository.existsByIdForLogin(code)) return code;
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to generate unique login code");
    }

    // ----------------------------------------------------------------------
    // 🔹 UPDATE DESIGNER PROFILE (by Designer)
    // ----------------------------------------------------------------------
    public DesignerResponse updateProfile(
            String bio,
            String portfolioUrl,
            String phone,
            String idForLogin,
            String positionStr
    ) {
        UUID userId = extractUserIdFromToken();

        DesignerDetail designer = designerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer profile not found"));

        if (designer.getDeletedAt() != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Designer has been deleted");

        if (bio != null) designer.setBio(bio);
        if (portfolioUrl != null) designer.setPortfolioUrl(portfolioUrl);

        if (idForLogin != null && !idForLogin.isBlank()) {
            String trimmed = idForLogin.trim();
            if (!trimmed.equalsIgnoreCase(designer.getIdForLogin())
                    && designerRepository.existsByIdForLogin(trimmed)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Login ID already in use");
            }
            designer.setIdForLogin(trimmed);
        }

        if (positionStr != null) {
            try {
                var pos = com.moden.modenapi.common.enums.Position.valueOf(positionStr);
                designer.setPosition(pos);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid position: " + positionStr);
            }
        }

        update(designer);
        return mapToRes(designer);
    }


    // ----------------------------------------------------------------------
    // 🔹 GET CURRENT DESIGNER PROFILE (via JWT)
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public DesignerResponse getCurrentDesignerProfile() {
        UUID userId = extractUserIdFromToken();

        DesignerDetail designer = designerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer profile not found"));

        if (designer.getDeletedAt() != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Designer has been deleted");

        return mapToRes(designer);
    }

    // ----------------------------------------------------------------------
    // 🔹 GET DESIGNER BY ID (Admin / Studio)
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public DesignerResponse getProfile(UUID designerId) {
        DesignerDetail designer = designerRepository.findActiveById(designerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found or deleted"));
        return mapToRes(designer);
    }

    // ----------------------------------------------------------------------
    // 🔹 GET ALL DESIGNERS BY STUDIO
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<DesignerResponse> getAllDesignersByStudio(UUID studioId) {
        return designerRepository.findAllActiveByHairStudioId(studioId)
                .stream()
                .map(this::mapToRes)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------------
    // 🔹 DELETE DESIGNER (soft delete)
    // ----------------------------------------------------------------------
    public void deleteDesigner(UUID designerId) {
        DesignerDetail designer = designerRepository.findActiveById(designerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found or already deleted"));

        designer.setDeletedAt(Instant.now());
        update(designer);
    }

    // ----------------------------------------------------------------------
    // 🔹 Helpers
    // ----------------------------------------------------------------------
    private UUID extractUserIdFromToken() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7).trim();
        if (!jwtProvider.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        return UUID.fromString(jwtProvider.getUserId(token));
    }

    private DesignerResponse mapToRes(DesignerDetail d) {
        return new DesignerResponse(
                d.getId(),
                d.getUserId(),
                d.getHairStudioId(),
                d.getIdForLogin(),
                d.getBio(),
                d.getPortfolioUrl(),
                d.getRole(),
                d.getPosition()
        );
    }
}
