package com.moden.modenapi.modules.auth.service;

import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.modules.auth.dto.UserMeFullResponse;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.customer.dto.CustomerResponse;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import com.moden.modenapi.modules.designer.dto.DesignerResponse;
import com.moden.modenapi.modules.designer.model.DesignerDetail;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthMeService {

    private final UserRepository userRepo;
    private final HairStudioDetailRepository studioRepo;
    private final DesignerDetailRepository designerRepo;
    private final CustomerDetailRepository customerRepo;

    /** ✅ detail-only (HAIR_STUDIO/DESIGNER/CUSTOMER), ADMIN이면 기본 프로필(UserMeResponse) 반환 */
    public Object getMePayload(UUID userId, List<String> authorities) {
        var base = computeBase(userId, authorities);

        switch (base.role) {
            case "HAIR_STUDIO" -> {
                var s = base.studio;
                if (s == null) return buildAdminLikeBase(base); // 방어
                var owner = base.user;
                return new StudioRes(
                        s.getId(),
                        s.getUserId(),
                        owner != null ? owner.getFullName() : null,
                        owner != null ? owner.getPhone()    : null,
                        s.getIdForLogin(),
                        s.getBusinessNo(),
                        s.getOwnerName(),
                        s.getStudioPhone(),
                        s.getAddress(),
                        s.getDescription(),
                        s.getProfileImageUrl(),
                        s.getLogoImageUrl(),
                        s.getBannerImageUrl(),
                        s.getInstagramUrl(),
                        s.getNaverUrl(),
                        s.getKakaoUrl(),
                        s.getParkingInfo(),
                        s.getLatitude(),
                        s.getLongitude()
                );
            }
            case "DESIGNER" -> {
                var d = base.designer;
                if (d == null) return buildAdminLikeBase(base);
                var u = base.user;
                return new DesignerResponse(
                        d.getId(),
                        d.getUserId(),
                        d.getHairStudioId(),
                        d.getIdForLogin(),
                        u != null ? Role.valueOf(u.getPhone()) : null,
                        u != null ? String.valueOf(u.getRole()) : null,
                        d.getBio(),
                        Collections.emptyList(), // 필요 시 포트폴리오 채우기
                        d.getCreatedAt(),
                        d.getUpdatedAt()
                );
            }
            case "CUSTOMER" -> {
                var c = base.customer;
                if (c == null) return buildAdminLikeBase(base);
                var u = base.user;
                return new CustomerResponse(
                        c.getId(),
                        u != null ? u.getFullName() : null,
                        u != null ? u.getPhone()    : null,
                        u != null ? u.getRole()     : null,
                        c.getEmail(),
                        c.getGender(),
                        c.getBirthdate(),
                        c.getAddress(),
                        c.getProfileImageUrl(),
                        c.getVisitReason(),
                        c.isConsentMarketing(),
                        c.getDesignerId()
                );
            }
            default -> {
                // ADMIN 또는 USER: detail 없이 기본 프로필 간단형 반환
                return buildAdminLikeBase(base);
            }
        }
    }

    // ---- 내부 공용 ----
    private record BaseAgg(
            User user,
            String role,
            UUID detailId,
            String detailName,
            HairStudioDetail studio,
            DesignerDetail designer,
            CustomerDetail customer
    ) {}

    private BaseAgg computeBase(UUID userId, List<String> authorities) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        var page1   = PageRequest.of(0, 1);
        var studio  = studioRepo  .findActiveByUserIdOrderByUpdatedDesc(userId, page1).stream().findFirst().orElse(null);
        var designer= designerRepo.findActiveByUserIdOrderByUpdatedDesc(userId, page1).stream().findFirst().orElse(null);
        var customer= customerRepo.findActiveByUserIdOrderByUpdatedDesc(userId, page1).stream().findFirst().orElse(null);

        String role = resolvePrimaryRole(authorities, studio, designer, customer,
                u.getRole() != null ? u.getRole().name() : null);

        UUID detailId = null;
        String detailName = null;

        switch (role) {
            case "HAIR_STUDIO" -> {
                if (studio != null) {
                    detailId = studio.getId();
                    detailName = firstNonBlank(
                            safe(studio.getOwnerName()),
                            safe(studio.getIdForLogin()),
                            safe(u.getFullName()),
                            safe(u.getPhone()),
                            u.getId().toString()
                    );
                }
            }
            case "DESIGNER" -> {
                if (designer != null) {
                    detailId = designer.getId();
                    detailName = firstNonBlank(
                            safe(u.getFullName()),
                            safe(u.getPhone()),
                            u.getId().toString()
                    );
                }
            }
            case "CUSTOMER" -> {
                if (customer != null) {
                    detailId = customer.getId();
                    detailName = firstNonBlank(
                            safe(u.getFullName()),
                            safe(u.getPhone()),
                            u.getId().toString()
                    );
                }
            }
            default -> {
                detailName = firstNonBlank(safe(u.getFullName()), safe(u.getPhone()), u.getId().toString());
            }
        }

        return new BaseAgg(u, role, detailId, detailName, studio, designer, customer);
    }

    private Object buildAdminLikeBase(BaseAgg base) {
        // ADMIN/USER 용 간단형 (detail 없이)
        return UserMeFullResponse.builder()
                .userId(base.user.getId())
                .fullName(base.user.getFullName())
                .phone(base.user.getPhone())
                .role(base.role)
                .detailId(base.detailId)
                .detailName(base.detailName)
                .createdAt(base.user.getCreatedAt())
                .build();
    }

    private static String safe(String s) { return (s == null || s.isBlank()) ? null : s; }
    private static String firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }

    private String resolvePrimaryRole(List<String> authorities,
                                      HairStudioDetail studio,
                                      DesignerDetail designer,
                                      CustomerDetail customer,
                                      String userRoleFallback) {
        if (authorities != null) {
            if (contains(authorities, "ROLE_ADMIN")) return "ADMIN";
            if (contains(authorities, "ROLE_HAIR_STUDIO") && studio != null) return "HAIR_STUDIO";
            if (contains(authorities, "ROLE_DESIGNER") && designer != null)   return "DESIGNER";
            if (contains(authorities, "ROLE_CUSTOMER") && customer != null)   return "CUSTOMER";
        }
        if (studio != null)   return "HAIR_STUDIO";
        if (designer != null) return "DESIGNER";
        if (customer != null) return "CUSTOMER";
        if (StringUtils.hasText(userRoleFallback)) return userRoleFallback;
        return "USER";
    }
    private boolean contains(List<String> authorities, String target) {
        if (authorities == null) return false;
        for (var a : authorities) if (target.equals(a)) return true;
        return false;
    }
}
