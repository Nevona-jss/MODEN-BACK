package com.moden.modenapi.modules.designer.service;

import com.moden.modenapi.common.enums.DesignerStatus;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.common.utils.IdGenerator;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.auth.service.AuthLocalService;
import com.moden.modenapi.modules.designer.dto.DesignerCreateDto;
import com.moden.modenapi.modules.designer.dto.DesignerResponse;
import com.moden.modenapi.modules.designer.dto.PortfolioItemRes;
import com.moden.modenapi.modules.designer.model.DesignerDetail;
import com.moden.modenapi.modules.designer.model.DesignerPortfolioItem;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.designer.repository.DesignerPortfolioItemRepository;
import com.moden.modenapi.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.moden.modenapi.common.enums.Role.DESIGNER;

@Service
@RequiredArgsConstructor
@Transactional
public class DesignerService extends BaseService<DesignerDetail> {

    private final DesignerDetailRepository designerRepository;
    private final DesignerPortfolioItemRepository portfolioRepository;
    private final UserRepository userRepository;
    private final AuthLocalService authLocalService;
    private final JwtProvider jwtProvider;
    private final HttpServletRequest request;

    @Override
    protected DesignerDetailRepository getRepository() {
        return designerRepository;
    }

    // ----------------------------------------------------------------------
    // CREATE (by Studio/Admin)
    // ----------------------------------------------------------------------
    public DesignerResponse createDesigner(DesignerCreateDto req) {
        UUID studioId = req.hairStudioId();
        if (studioId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hairStudioId is required");
        }

        String loginCode = (req.idForLogin() == null || req.idForLogin().isBlank())
                ? generateUniqueDesignerCode()
                : req.idForLogin().trim();

        if (designerRepository.existsByIdForLogin(loginCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Designer login ID already exists: " + loginCode);
        }

        // 1) User with role DESIGNER
        String phone = (req.phone() != null && !req.phone().isBlank()) ? req.phone().trim() : "UNKNOWN";
        User user = User.builder()
                .phone(phone)
                .role(DESIGNER)
                .build();
        userRepository.save(user);

        // 2) DesignerDetail (portfolio IDs initially empty)
        DesignerDetail designer = DesignerDetail.builder()
                .userId(user.getId())
                .hairStudioId(studioId)
                .idForLogin(loginCode)
                .bio(req.bio())
                .position(req.position())
                .status(DesignerStatus.WORKING)
                .portfolioItemIds(new ArrayList<>())   // ✅ use list, not JSON
                .build();

        create(designer);

        // 3) password → local store
        if (req.password() != null && !req.password().isBlank()) {
            authLocalService.createOrUpdatePassword(user.getId(), req.password());
        }

        return mapToRes(designer, user);
    }

    // ----------------------------------------------------------------------
    // UPDATE (by Designer)
    // ----------------------------------------------------------------------
    public DesignerResponse updateProfile(String bio,
                                          String portfolioUrlDeprecated, // kept for backward compat; ignored
                                          String phone,
                                          String idForLogin,
                                          String positionStr) {
        UUID userId = extractUserIdFromToken();

        DesignerDetail d = designerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer profile not found"));
        if (d.getDeletedAt() != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Designer has been deleted");

        if (bio != null) d.setBio(bio);

        if (idForLogin != null && !idForLogin.isBlank()) {
            String trimmed = idForLogin.trim();
            if (!trimmed.equalsIgnoreCase(d.getIdForLogin()) && designerRepository.existsByIdForLogin(trimmed)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Login ID already in use");
            }
            d.setIdForLogin(trimmed);
        }

        if (positionStr != null && !positionStr.isBlank()) {
            try {
                var pos = com.moden.modenapi.common.enums.Position.valueOf(positionStr);
                d.setPosition(pos);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid position: " + positionStr);
            }
        }

        // optional: update phone on User
        if (phone != null && !phone.isBlank()) {
            User u = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            u.setPhone(phone.trim());
            userRepository.save(u);
        }

        update(d);

        // reload user for mapping
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapToRes(d, user);
    }

    // ----------------------------------------------------------------------
    // STATUS (근무중/휴가/교육) change
    // ----------------------------------------------------------------------
    public DesignerResponse changeStatus(UUID designerId, DesignerStatus status) {
        DesignerDetail d = designerRepository.findActiveById(designerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found or deleted"));
        d.setStatus(status);
        update(d);

        User u = userRepository.findById(d.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapToRes(d, u);
    }

    // ----------------------------------------------------------------------
    // PORTFOLIO ID management  (List<UUID> only)
    // ----------------------------------------------------------------------
    public DesignerResponse replacePortfolio(UUID designerId, List<UUID> newItemIds) {
        DesignerDetail d = designerRepository.findActiveById(designerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found or deleted"));

        d.setPortfolioItemIds(distinctIds(newItemIds));
        update(d);

        User u = userRepository.findById(d.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapToRes(d, u);
    }

    public DesignerResponse addPortfolio(UUID designerId, List<UUID> itemIdsToAdd) {
        DesignerDetail d = designerRepository.findActiveById(designerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found or deleted"));

        List<UUID> current = new ArrayList<>(Optional.ofNullable(d.getPortfolioItemIds()).orElseGet(ArrayList::new));
        current.addAll(itemIdsToAdd);
        d.setPortfolioItemIds(distinctIds(current));
        update(d);

        User u = userRepository.findById(d.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapToRes(d, u);
    }

    public DesignerResponse removePortfolio(UUID designerId, UUID itemId) {
        DesignerDetail d = designerRepository.findActiveById(designerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found or deleted"));

        List<UUID> current = new ArrayList<>(Optional.ofNullable(d.getPortfolioItemIds()).orElseGet(ArrayList::new));
        boolean changed = current.removeIf(id -> id.equals(itemId));
        if (!changed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not attached to this designer");
        }
        d.setPortfolioItemIds(current);
        update(d);

        User u = userRepository.findById(d.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapToRes(d, u);
    }

    // ----------------------------------------------------------------------
    // READ
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public DesignerResponse getCurrentDesignerProfile() {
        UUID userId = extractUserIdFromToken();

        DesignerDetail d = designerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer profile not found"));
        if (d.getDeletedAt() != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Designer has been deleted");

        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapToRes(d, u);
    }

    @Transactional(readOnly = true)
    public DesignerResponse getProfile(UUID designerId) {
        DesignerDetail d = designerRepository.findActiveById(designerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found or deleted"));
        User u = userRepository.findById(d.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapToRes(d, u);
    }

    @Transactional(readOnly = true)
    public List<DesignerResponse> getAllDesignersByStudio(UUID studioId) {
        return designerRepository.findAllActiveByHairStudioId(studioId).stream()
                .map(d -> {
                    User u = userRepository.findById(d.getUserId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                    return mapToRes(d, u);
                })
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------------
    // DELETE (soft)
    // ----------------------------------------------------------------------
    public void deleteDesigner(UUID designerId) {
        DesignerDetail d = designerRepository.findActiveById(designerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found or already deleted"));
        d.setDeletedAt(Instant.now());
        update(d);
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------
    private String generateUniqueDesignerCode() {
        for (int i = 0; i < 5; i++) {
            String code = IdGenerator.generateId("DS");
            if (!designerRepository.existsByIdForLogin(code)) return code;
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to generate unique login code");
    }

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

    private List<UUID> distinctIds(List<UUID> ids) {
        if (ids == null) return Collections.emptyList();
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    // Mapping: join user + resolve portfolio items (ordered)
    private DesignerResponse mapToRes(DesignerDetail d, User u) {
        List<UUID> ids = Optional.ofNullable(d.getPortfolioItemIds()).orElseGet(List::of);

        List<DesignerPortfolioItem> items = ids.isEmpty()
                ? List.of()
                : portfolioRepository.findAllByIdIn(ids); // assumes you created this repo method

        // keep order as in ids
        Map<UUID, DesignerPortfolioItem> byId = items.stream()
                .collect(Collectors.toMap(DesignerPortfolioItem::getId, it -> it));
        List<PortfolioItemRes> portfolio = new ArrayList<>();
        for (UUID id : ids) {
            DesignerPortfolioItem it = byId.get(id);
            if (it != null && it.getDeletedAt() == null) {
                portfolio.add(new PortfolioItemRes(it.getId(), it.getImageUrl(), it.getCaption()));
            }
        }

        return new DesignerResponse(
                d.getId(),
                d.getUserId(),
                d.getHairStudioId(),
                d.getIdForLogin(),
                u.getPhone(),
                u.getRole(),          // Role lives on User
                d.getBio(),
                portfolio,
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
