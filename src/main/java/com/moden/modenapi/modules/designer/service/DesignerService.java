package com.moden.modenapi.modules.designer.service;

import com.moden.modenapi.common.enums.DesignerStatus;
import com.moden.modenapi.common.enums.Position;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.enums.Weekday;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.common.service.StudioContextService;
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
import java.util.stream.Collectors;

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
    private final DesignerPortfolioService  portfolioService;
    private final StudioContextService studioContextService;

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
    public DesignerResponse createDesigner(
            HttpServletRequest request,
            DesignerCreateDto req
    ) {
        ensureStudioRole();

        UUID currentUserId = getCurrentUserIdOrThrow();
        var myStudios =
                hairStudioRepo.findActiveByUserIdOrderByUpdatedDesc(currentUserId, PageRequest.of(0, 1));
        if (myStudios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Studio not found for current user");
        }

        var studio = myStudios.get(0);
        UUID studioUserId = studio.getUserId();  // ✅ 스튜디오의 User ID
        String rawPwd = req.password() == null ? "" : req.password().trim();

        if (rawPwd.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }

        String phone = req.phone() == null ? "" : req.phone().trim();
        if (phone.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone is required");
        }
        String fullName = req.fullName() == null ? null : req.fullName().trim();

        // 1) USER upsert
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

        // 2) AuthLocal
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

        // 3) Idempotency (user ↔ designer)
        var existing = designerRepository.findByUserId(user.getId()).orElse(null);
        if (existing != null) {
            if (studioUserId.equals(existing.getHairStudioId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Designer already belongs to this studio");
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Designer belongs to another studio");
            }
        }

        // 4) idForLogin: endi DTO'dan
        String loginCode = req.idForLogin() == null ? "" : req.idForLogin().trim();
        if (loginCode.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idForLogin is required");
        }

        // loginCode unique bo‘lishi shart
        if (designerRepository.existsByIdForLogin(loginCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "idForLogin already in use");
        }

        // 5) DesignerDetail (hali persist emas)
        DesignerDetail d = DesignerDetail.builder()
                .userId(user.getId())
                .hairStudioId(studioUserId)
                .idForLogin(loginCode)
                .position(req.position() != null ? req.position() : Position.DESIGNER)
                .status(req.status() != null ? req.status() : DesignerStatus.WORKING)
                .portfolioItemIds(new ArrayList<>())
                .build();

        if (req.daysOff() != null && !req.daysOff().isEmpty()) {
            var offDays = req.daysOff().stream()
                    .map(Weekday::fromCode)
                    .toList();
            d.setDaysOff(offDays);
        }

        DesignerDetail saved = create(d); // DesignerDetail saqlandi

        // 6) Portfolio URL lar bo‘lsa → entity + response ga o‘tkazamiz
        List<String> portfolioUrls = List.of();

        if (req.portfolio() != null && !req.portfolio().isEmpty()) {
            List<DesignerPortfolioItem> entities = req.portfolio().stream()
                    .map(url -> DesignerPortfolioItem.builder()
                            .designerId(saved.getId())
                            .imageUrl(url)
                            .caption(null)
                            .build())
                    .toList();

            portfolioRepo.saveAll(entities);

            portfolioUrls = entities.stream()
                    .map(DesignerPortfolioItem::getImageUrl)
                    .toList();
        }

        return mapToRes(saved, user, portfolioUrls);


    }

    /** Designer o‘zi profilini yangilaydi */
    @Transactional
    public DesignerResponse updateProfile(HttpServletRequest request, DesignerUpdateReq req) {
        UUID currentUserId = getCurrentUserIdOrThrow();

        DesignerDetail d = designerRepository.findByUserIdAndDeletedAtIsNull(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Designer not found"
                ));

        User user = userRepository.findById(d.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));

        // --- User 필드 업데이트 (fullName, phone) ---
        if (req.fullName() != null) {
            user.setFullName(req.fullName());
        }
        if (req.phone() != null) {
            user.setPhone(req.phone());
        }

        // --- DesignerDetail 필드 업데이트 (idForLogin, position, status, daysOff) ---
        if (req.idForLogin() != null) {
            d.setIdForLogin(req.idForLogin());
        }
        if (req.position() != null) {
            d.setPosition(Position.valueOf(req.position()));
        }
        if (req.status() != null) {
            d.setStatus(DesignerStatus.valueOf(req.status()));
        }
        if (req.daysOff() != null) {
            d.setDaysOff(toWeekdayList(req.daysOff()));
        }

        // --- Portfolio (String URL 배열 → 아이템 생성 + ID 연결) ---
        if (req.portfolio() != null) {
            portfolioService.replaceWithUrls(d.getId(), req.portfolio());
        }

        designerRepository.save(d);
        userRepository.save(user);

        // 응답용 포트폴리오: URL string list
        var portfolioUrls = portfolioService.getPortfolio(d.getId()).stream()
                .map(DesignerPortfolioItem::getImageUrl)
                .toList();

        return mapToRes(d, user, portfolioUrls);  // ✅ 한 번만 리턴
    }



    private List<Weekday> toWeekdayList(List<Integer> codes) {
        if (codes == null) return new ArrayList<>();
        List<Weekday> result = new ArrayList<>();
        for (Integer c : codes) {
            if (c == null) continue;
            result.add(Weekday.fromCode(c));  // 0..6 → MON..SUN
        }
        return result;
    }



    @Transactional
    public DesignerResponse updateProfileByStudio(
            HttpServletRequest request,
            UUID userId,
            DesignerUpdateReq req
    ) {
        ensureStudioRole();
        UUID studioId = getStudioIdFromCurrentStudio();

        DesignerDetail d = designerRepository
                .findByUserIdAndHairStudioIdAndDeletedAtIsNull(userId, studioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Designer not found in your studio"
                ));

        User user = userRepository.findById(d.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));

        // --- User 필드 (fullName, phone) ---
        if (req.fullName() != null) {
            user.setFullName(req.fullName());
        }
        if (req.phone() != null) {
            user.setPhone(req.phone());
        }

        // --- DesignerDetail 필드 (idForLogin, position, status, daysOff) ---
        if (req.idForLogin() != null) {
            d.setIdForLogin(req.idForLogin());
        }
        if (req.position() != null) {
            d.setPosition(Position.valueOf(req.position()));
        }
        if (req.status() != null) {
            d.setStatus(DesignerStatus.valueOf(req.status()));
        }
        if (req.daysOff() != null) {
            d.setDaysOff(toWeekdayList(req.daysOff()));
        }

        // --- Portfolio (URL 배열) ---
        if (req.portfolio() != null) {
            portfolioService.replaceWithUrls(d.getId(), req.portfolio());
        }

        designerRepository.save(d);
        userRepository.save(user);

        var portfolioUrls = portfolioService.getPortfolio(d.getId()).stream()
                .map(DesignerPortfolioItem::getImageUrl)
                .toList();

        return mapToRes(d, user, portfolioUrls);   // ✅ List<String>
    }





    /* ===================== READ ===================== */

    @Transactional(readOnly = true)
    public DesignerResponse getProfile(UUID userId) {
        var d = designerRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found"));

        var user = userRepository.findById(d.getUserId()).orElse(null);

        var portfolioUrls = portfolioRepo
                .findAllByDesignerIdAndDeletedAtIsNullOrderByCreatedAtAsc(d.getId())
                .stream()
                .map(DesignerPortfolioItem::getImageUrl)
                .toList();

        return mapToRes(d, user, portfolioUrls);
    }


    @Transactional(readOnly = true)
    public List<String> getPortfolio(UUID designerId) {
        return portfolioRepo.findAllByDesignerIdAndDeletedAtIsNullOrderByCreatedAtAsc(designerId)
                .stream()
                .map(DesignerPortfolioItem::getImageUrl)
                .toList();
    }


    /* ===================== DELETE (SOFT) ===================== */

    /** Studio o‘z dizaynerini SOFT DELETE qiladi */
    @Transactional
    public void deleteDesigner(HttpServletRequest request, UUID userId) {
        ensureStudioRole();
        UUID studioId = getStudioIdFromCurrentStudio();

        // 1) user borligini tekshirish
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));

        // 2) shu user aynan shu studiodagi designer ekanini tekshirish
        DesignerDetail d = designerRepository
                .findByUserIdAndHairStudioIdAndDeletedAtIsNull(userId, studioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Designer not found in your studio"
                ));

        // 3) ikkala jadvalni ham soft delete
        softDeleteUser(user.getId());
        softDeleteDesignerDetail(d.getId());
    }

    public void softDeleteUser(UUID userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        u.setDeletedAt(Instant.now());
        userRepository.save(u);
    }

    public void softDeleteDesignerDetail(UUID designerDetailId) {
        DesignerDetail d = designerRepository.findById(designerDetailId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        d.setDeletedAt(Instant.now());
        designerRepository.save(d);
    }


    @Transactional(readOnly = true)
    public List<DesignerResponse> listDesignersForCurrentStudio(
            String keyword,
            boolean onlyActive
    ) {
        // 1) 현재 로그인한 actor 기준으로 studioUserId 가져오기
        UUID studioId = studioContextService.resolveStudioIdForCurrentUser();

        // 2) 스튜디오에 속한 디자이너 목록 조회 (active 여부에 따라 분기)
        List<DesignerDetail> designers = onlyActive
                ? designerRepository.findAllActiveByHairStudioIdOrderByUpdatedDesc(studioId)
                : designerRepository.findAllByHairStudioIdOrderByUpdatedAtDesc(studioId);

        if (designers.isEmpty()) {
            return List.of();
        }

        // 3) userId → User 매핑 (N+1 방지)
        List<UUID> userIds = designers.stream()
                .map(DesignerDetail::getUserId)
                .filter(Objects::nonNull)
                .toList();

        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<DesignerResponse> result = new ArrayList<>();

        for (DesignerDetail d : designers) {
            User user = userMap.get(d.getUserId());

            // 4) keyword 필터 (이름 / 이메일 기준)
            if (keyword != null && !keyword.isBlank()) {
                String k = keyword.toLowerCase();

                String name = "";
                String email = "";
                if (user != null) {
                    name = Optional.ofNullable(user.getFullName()).orElse("").toLowerCase();
                    // email 필드 있으면 여기에 추가
                    // email = Optional.ofNullable(user.getEmail()).orElse("").toLowerCase();
                }

                if (!name.contains(k) && !email.contains(k)) {
                    continue; // 이 디자이너는 결과에서 제외
                }
            }

            // 5) 포트폴리오 이미지 URL 목록
            var portfolioUrls = portfolioRepo
                    .findAllByDesignerIdAndDeletedAtIsNullOrderByCreatedAtAsc(d.getId())
                    .stream()
                    .map(DesignerPortfolioItem::getImageUrl)
                    .toList();

            // 6) 최종 DTO 매핑
            DesignerResponse res = mapToRes(d, user, portfolioUrls);
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


    private DesignerResponse mapToRes(
            DesignerDetail d,
            User user,
            List<String> portfolioUrls   // ✅ List<String>
    ) {
        UUID userId   = (user != null) ? user.getId()        : null;
        String fullName = (user != null) ? user.getFullName() : null;
        String phone    = (user != null) ? user.getPhone()    : null;
        String role    = (user != null) ? String.valueOf(user.getRole()) : null;

        return new DesignerResponse(
                userId,
                d.getHairStudioId(),
                d.getIdForLogin(),
                fullName,
                phone,
                d.getPosition(),
                role,
                d.getStatus(),
                d.getDaysOff(),
                portfolioUrls
        );
    }

}
