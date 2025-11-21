package com.moden.modenapi.modules.designer.service;

import com.moden.modenapi.common.enums.DesignerStatus;
import com.moden.modenapi.common.enums.Position;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.enums.Weekday;
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

    /**
     * Studio dizayner yaratadi.
     * hairStudioId – token’dagi current HAIR_STUDIO foydalanuvchisidan olinadi.
     */
    @Transactional
    public DesignerResponse createDesigner(HttpServletRequest request, DesignerCreateDto req) {
        ensureStudioRole();

        UUID currentUserId = getCurrentUserIdOrThrow();
        var myStudios =
                hairStudioRepo.findActiveByUserIdOrderByUpdatedDesc(currentUserId, PageRequest.of(0, 1));
        if (myStudios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Studio not found for current user");
        }
        var studio = myStudios.get(0);
        UUID studioId = studio.getId();

        String rawPwd = req.password() == null ? "" : req.password().trim();
        if (rawPwd.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }

        String phone = req.phone() == null ? "" : req.phone().trim();
        if (phone.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone is required");
        }
        String fullName = req.fullName() == null ? null : req.fullName().trim();

        // 1) USER upsert (majburiy DESIGNER)
        User user = userRepository.findByPhone(phone).orElse(null);
        if (user == null) {
            user = User.builder()
                    .fullName(fullName)
                    .phone(phone)
                    .role(Role.DESIGNER)
                    .build();
        } else {
            user.setFullName(fullName);
            user.setRole(Role.DESIGNER); // majburan DESIGNER
        }
        user = userRepository.save(user);

        // 2) AuthLocal (BCrypt hash)
        AuthLocal authLocal = authLocalRepository.findByUserId(user.getId()).orElse(null);
        if (authLocal == null) {
            authLocal = AuthLocal.builder()
                    .userId(user.getId())
                    .passwordHash(passwordEncoder.encode(rawPwd))
                    .passwordUpdatedAt(Instant.now())
                    .build();
        } else if (!passwordEncoder.matches(rawPwd, authLocal.getPasswordHash())) {
            authLocal.setPasswordHash(passwordEncoder.encode(rawPwd));
            authLocal.setPasswordUpdatedAt(Instant.now());
        }
        authLocalRepository.save(authLocal);

        // 3) Idempotency: shu user allaqachon designer emasligini tekshirish
        var existing = designerRepository.findByUserId(user.getId()).orElse(null);
        if (existing != null) {
            if (studioId.equals(existing.getHairStudioId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Designer already belongs to this studio");
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Designer belongs to another studio");
            }
        }

        // 4) Unique login code
        String loginCode = generateDesignerLoginCode();
        int guard = 0;
        while (designerRepository.existsByIdForLogin(loginCode)) {
            if (++guard > 20) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot generate unique login ID");
            }
            loginCode = generateDesignerLoginCode();
        }

        // 5) DesignerDetail
        DesignerDetail d = DesignerDetail.builder()
                .userId(user.getId())
                .hairStudioId(studioId)
                .idForLogin(loginCode)
                .position(req.position() != null ? req.position() : Position.DESIGNER)
                .status(req.status() != null ? req.status() : DesignerStatus.WORKING)
                .portfolioItemIds(new ArrayList<>())
                .build();

        // ✅ daysOff: Integer kod → Weekday list
        if (req.daysOff() != null && !req.daysOff().isEmpty()) {
            List<Weekday> offDays = req.daysOff().stream()
                    .map(Weekday::fromCode)     // 0..6 → Weekday enum
                    .toList();
            d.setDaysOff(offDays);
        }

        d = create(d); // persist

        // 6) Hozircha portfolio bo‘sh
        List<PortfolioItemRes> items = List.of();

        // 7) Javob
        return mapToRes(d, user, items);
    }

    /* ===================== UPDATE (SELF) ===================== */

    /** Designer o‘zi profilini yangilaydi */
    public DesignerResponse updateProfile(HttpServletRequest request, DesignerUpdateReq req) {
        UUID currentUserId = getCurrentUserIdOrThrow();

        DesignerDetail d = designerRepository.findByUserIdAndDeletedAtIsNull(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found"));

        if (req.phone() != null) {
            userRepository.findById(d.getUserId()).ifPresent(u -> {
                u.setPhone(req.phone());
                userRepository.save(u);
            });
        }

        if (req.position() != null) {
            try {
                d.setPosition(Position.valueOf(req.position()));
            } catch (Exception ignore) {}
        }

        if (req.status() != null) {
            try {
                d.setStatus(DesignerStatus.valueOf(req.status()));
            } catch (Exception ignore) {}
        }

        if (req.daysOff() != null) {
            List<Weekday> offDays = req.daysOff().stream()
                    .map(Weekday::fromCode)
                    .toList();
            d.setDaysOff(offDays);
        }

        update(d);

        var user = userRepository.findById(d.getUserId()).orElse(null);
        var items = portfolioRepo
                .findAllByDesignerIdAndDeletedAtIsNullOrderByCreatedAtAsc(d.getId())
                .stream()
                .map(it -> new PortfolioItemRes(it.getId(), it.getImageUrl(), it.getCaption()))
                .toList();

        return mapToRes(d, user, items);
    }

    /* ===================== READ ===================== */

    @Transactional(readOnly = true)
    public DesignerResponse getProfile(UUID designerId) {
        var d = designerRepository.findActiveById(designerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found"));

        var user = userRepository.findById(d.getUserId()).orElse(null);
        var items = portfolioRepo
                .findAllByDesignerIdAndDeletedAtIsNullOrderByCreatedAtAsc(d.getId())
                .stream()
                .map(it -> new PortfolioItemRes(it.getId(), it.getImageUrl(), it.getCaption()))
                .toList();

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

    /** Studio o‘z dizaynerini SOFT DELETE qiladi */
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

    /* ===================== LIST: CURRENT STUDIO ===================== */

    @Transactional(readOnly = true)
    public List<DesignerResponse> listDesignersForCurrentStudio(
            String keyword,
            boolean onlyActive
    ) {
        // 1) current HAIR_STUDIO → studioId (sendagi util method)
        UUID studioId = getStudioIdFromCurrentStudio();

        // 2) Shu studiodagi barcha designerlar
        List<DesignerDetail> designers =
                designerRepository.findAllActiveByHairStudioIdOrderByUpdatedDesc(studioId);

        if (designers.isEmpty()) {
            return List.of();
        }

        List<DesignerResponse> result = new ArrayList<>();

        for (DesignerDetail d : designers) {

            // --- onlyActive: deletedAt == null ni active deb hisoblaymiz ---
            if (onlyActive && d.getDeletedAt() != null) {
                continue;
            }

            User user = userRepository.findById(d.getUserId()).orElse(null);

            var items = portfolioRepo
                    .findAllByDesignerIdAndDeletedAtIsNullOrderByCreatedAtAsc(d.getId())
                    .stream()
                    .map(it -> new PortfolioItemRes(it.getId(), it.getImageUrl(), it.getCaption()))
                    .toList();

            DesignerResponse res = mapToRes(d, user, items);

            // --- keyword filter (User.fullName / email / DesignerDetail.nickname ...) ---
            if (keyword != null && !keyword.isBlank()) {
                String k = keyword.toLowerCase();

                String name = "";
                String email = "";
                if (user != null) {
                    name  = Optional.ofNullable(user.getFullName()).orElse("").toLowerCase();
                }

                if (!name.contains(k) && !email.contains(k)) {
                    continue;
                }
            }

            result.add(res);
        }

        return result;
    }


    /* ===================== Helpers ===================== */

    private void ensureStudioRole() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean ok = auth != null
                && auth.getAuthorities() != null
                && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_HAIR_STUDIO".equals(a.getAuthority()));

        if (!ok) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only studio can perform this action");
        }
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
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Studio not found for this user");
        }
        return list.get(0).getId();
    }


    private static final SecureRandom RND = new SecureRandom();
    private String generateDesignerLoginCode() {
        // DS-ABCDE-12345
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(letters.charAt(RND.nextInt(letters.length())));
        }
        int digits = 10000 + RND.nextInt(90000);
        return "DS-" + sb + "-" + digits;
    }

    private DesignerResponse mapToRes(DesignerDetail d, User user, List<PortfolioItemRes> items) {

        Role effectiveRole = (user != null && user.getRole() != null)
                ? user.getRole()
                : Role.DESIGNER;

        String fullName = user != null ? user.getFullName() : null;
        String phone    = user != null ? user.getPhone()    : null;

        return new DesignerResponse(
                d.getUserId(),
                d.getHairStudioId(),
                d.getIdForLogin(),

                effectiveRole,
                fullName,
                phone,

                d.getPosition(),
                d.getStatus(),
                d.getDaysOff(),

                items,
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
