package com.moden.modenapi.modules.designer.service;

import com.moden.modenapi.common.enums.DesignerStatus;
import com.moden.modenapi.common.enums.Position;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.auth.model.AuthLocal;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.AuthLocalRepository;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.designer.dto.*;
import com.moden.modenapi.modules.designer.model.DesignerDetail;
import com.moden.modenapi.modules.designer.model.DesignerPortfolioItem;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.designer.repository.DesignerPortfolioItemRepository;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class DesignerService extends BaseService<DesignerDetail> {

    private final DesignerDetailRepository designerRepository;
    private final DesignerPortfolioItemRepository portfolioRepo;
    private final HairStudioDetailRepository hairStudioRepo;
    private final UserRepository userRepository;
    private final AuthLocalRepository authLocalRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    protected JpaRepository<DesignerDetail, UUID> getRepository() {
        return designerRepository;
    }

    /* ===================== CREATE ===================== */

    /** Studio creates designer. hairStudioId is taken from current studio (token principal). */
    public DesignerResponse createDesigner(HttpServletRequest request, DesignerCreateDto req) {
        ensureStudioRole();

        UUID currentUserId = getCurrentUserIdOrThrow();
        var myStudios = hairStudioRepo.findActiveByUserIdOrderByUpdatedDesc(currentUserId, PageRequest.of(0, 1));
        if (myStudios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Studio not found for current user");
        }
        UUID studioId = myStudios.get(0).getId();

        String rawPwd = req.password() == null ? "" : req.password().trim();
        if (rawPwd.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }

        String loginCode = generateDesignerLoginCode();
        int guard = 0;
        while (designerRepository.existsByIdForLogin(loginCode)) {
            if (++guard > 20) throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot generate unique login ID");
            loginCode = generateDesignerLoginCode();
        }

        // Upsert User (force role=DESIGNER)
        String phone = req.phone().trim();
        String fullName = req.fullName() == null ? null : req.fullName().trim();

        User user = userRepository.findByPhone(phone).orElse(null);
        if (user == null) {
            user = User.builder()
                    .fullName(fullName)
                    .phone(phone)
                    .role(Role.DESIGNER)
                    .build();
        } else {
            user.setFullName(fullName);
            user.setRole(Role.DESIGNER);
        }
        user = userRepository.save(user);

        // Save password (AuthLocal)
        AuthLocal authLocal = authLocalRepository.findByUserId(user.getId()).orElse(null);
        if (authLocal == null) {
            authLocal = AuthLocal.builder()
                    .userId(user.getId())
                    .passwordHash(passwordEncoder.encode(rawPwd))
                    .passwordUpdatedAt(Instant.now())
                    .build();
        } else {
            authLocal.setPasswordHash(passwordEncoder.encode(rawPwd));
            authLocal.setPasswordUpdatedAt(Instant.now());
        }
        authLocalRepository.save(authLocal);

        // DesignerDetail
        DesignerDetail d = DesignerDetail.builder()
                .userId(user.getId())
                .hairStudioId(studioId)
                .idForLogin(loginCode)
                .bio(req.bio())
                .position(req.position() != null ? req.position() : Position.DESIGNER)
                .status(req.status() != null ? req.status() : DesignerStatus.WORKING)
                .portfolioItemIds(new ArrayList<>())
                .build();

        d = create(d);

        return mapToRes(d, user, List.of());
    }

    /* ===================== UPDATE (SELF) ===================== */

    /** Designer updates own profile */
    public DesignerResponse updateOwnProfile(HttpServletRequest request, DesignerUpdateReq req) {
        UUID currentUserId = getCurrentUserIdOrThrow();

        DesignerDetail d = designerRepository.findByUserIdAndDeletedAtIsNull(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found"));

        if (req.bio() != null) d.setBio(req.bio());
        if (req.phone() != null) {
            userRepository.findById(d.getUserId()).ifPresent(u -> {
                u.setPhone(req.phone());
                userRepository.save(u);
            });
        }
        if (req.position() != null) {
            try { d.setPosition(Position.valueOf(req.position())); } catch (Exception ignore) {}
        }
        if (req.status() != null) {
            try { d.setStatus(DesignerStatus.valueOf(req.status())); } catch (Exception ignore) {}
        }
        update(d);

        var user = userRepository.findById(d.getUserId()).orElse(null);
        var items = portfolioRepo.findAllByDesignerIdAndDeletedAtIsNullOrderByCreatedAtAsc(d.getId())
                .stream().map(it -> new PortfolioItemRes(it.getId(), it.getImageUrl(), it.getCaption())).toList();

        return mapToRes(d, user, items);
    }

    /* ===================== READ ===================== */

    @Transactional(readOnly = true)
    public DesignerResponse getProfile(UUID designerId) {
        var d = designerRepository.findActiveById(designerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found"));

        var user = userRepository.findById(d.getUserId()).orElse(null);
        var items = portfolioRepo.findAllByDesignerIdAndDeletedAtIsNullOrderByCreatedAtAsc(d.getId())
                .stream().map(it -> new PortfolioItemRes(it.getId(), it.getImageUrl(), it.getCaption())).toList();

        return mapToRes(d, user, items);
    }

    @Transactional(readOnly = true)
    public List<PortfolioItemRes> getPortfolio(UUID designerId) {
        return portfolioRepo.findAllByDesignerIdAndDeletedAtIsNullOrderByCreatedAtAsc(designerId)
                .stream()
                .map(p -> new PortfolioItemRes(p.getId(), p.getImageUrl(), p.getCaption()))
                .toList();
    }

    /* ===================== DELETE (SOFT) ===================== */

    /** Studio deletes its own designer (SOFT DELETE only) */
    public void deleteDesigner(HttpServletRequest request, UUID designerId) {
        ensureStudioRole();
        UUID studioId = getStudioIdFromCurrentStudio();

        DesignerDetail d = designerRepository.findActiveById(designerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found"));

        if (!Objects.equals(studioId, d.getHairStudioId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Designer does not belong to your studio");
        }
        softDelete(d.getId()); // BaseEntity.deletedAt ga vaqt yoziladi
    }

    /* ===================== PORTFOLIO ===================== */

    /** Portfolio add (Studio of this designer, or the designer himself) */
    public List<PortfolioItemRes> addPortfolioItems(HttpServletRequest request, UUID designerId, PortfolioAddReq req) {
        // Studio yoki o‘zi – bu tekshiruvni controller darajasida role bilan cheklash mumkin.
        var d = designerRepository.findActiveById(designerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found"));

        List<String> paths = Optional.ofNullable(req.paths()).orElseGet(List::of);
        List<String> captions = Optional.ofNullable(req.captions()).orElseGet(List::of);

        if (paths.isEmpty()) return getPortfolio(designerId);

        List<UUID> newIds = new ArrayList<>();
        IntStream.range(0, paths.size()).forEach(i -> {
            String url = "/uploads/" + paths.get(i); // frontend /api/uploads dan olgan relPath
            String caption = (i < captions.size()) ? captions.get(i) : null;

            DesignerPortfolioItem item = DesignerPortfolioItem.builder()
                    .designerId(designerId)
                    .imageUrl(url)
                    .caption(caption)
                    .build();
            portfolioRepo.save(item);
            newIds.add(item.getId());
        });

        List<UUID> order = Optional.ofNullable(d.getPortfolioItemIds()).orElseGet(ArrayList::new);
        order.addAll(newIds);
        d.setPortfolioItemIds(order);
        update(d);

        return getPortfolio(designerId);
    }

    /* ===================== Helpers ===================== */

    private void ensureStudioRole() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean ok = auth != null && auth.getAuthorities() != null &&
                auth.getAuthorities().stream().anyMatch(a -> "ROLE_HAIR_STUDIO".equals(a.getAuthority()));
        if (!ok) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only studio can perform this action");
    }

    private UUID getCurrentUserIdOrThrow() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No current user");
        }
        try {
            return UUID.fromString(auth.getPrincipal().toString());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
        }
    }

    private UUID getStudioIdFromCurrentStudio() {
        UUID ownerUserId = getCurrentUserIdOrThrow();
        var list = hairStudioRepo.findActiveByUserIdOrderByUpdatedDesc(ownerUserId, PageRequest.of(0, 1));
        if (list.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Studio not found for this user");
        return list.get(0).getId();
    }

    private static final SecureRandom RND = new SecureRandom();
    private String generateDesignerLoginCode() {
        // DS-ABCDE-12345
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(letters.charAt(RND.nextInt(letters.length())));
        int digits = 10000 + RND.nextInt(90000);
        return "DS-" + sb + "-" + digits;
    }

    private DesignerResponse mapToRes(DesignerDetail d, User user, List<PortfolioItemRes> items) {
        return new DesignerResponse(
                d.getId(),
                d.getUserId(),
                d.getHairStudioId(),
                d.getIdForLogin(),
                user != null ? Role.valueOf(user.getPhone()) : null,
                user != null ? String.valueOf(user.getRole()) : null,
                d.getBio(),
                items,
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
