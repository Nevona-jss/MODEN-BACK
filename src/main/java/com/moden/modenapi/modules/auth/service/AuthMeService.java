package com.moden.modenapi.modules.auth.service;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.modules.auth.dto.UserMeFullResponse;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.customer.dto.CustomerResponseForMe;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import com.moden.modenapi.modules.designer.dto.DesignerResponse;
import com.moden.modenapi.modules.designer.model.DesignerDetail;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.reservation.repository.ReservationRepository;
import com.moden.modenapi.modules.studio.dto.StudioBrief;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import com.moden.modenapi.modules.studioservice.repository.StudioServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthMeService {

    private final UserRepository userRepo;
    private final HairStudioDetailRepository studioRepo;
    private final DesignerDetailRepository designerRepo;
    private final CustomerDetailRepository customerRepo;
    private final ReservationRepository reservationRepo;
    private final StudioServiceRepository studioServiceRepository;

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
                        s.getLogoImageUrl(),
                        s.getBannerImageUrl(),
                        s.getNaverUrl(),
                        s.getKakaoUrl(),
                        s.getLatitude(),
                        s.getLongitude(),
                        owner != null ? owner.getRole() : null

                        );
            }
            case "DESIGNER" -> {
                var d = base.designer;
                if (d == null) return buildAdminLikeBase(base);
                var u = base.user;

                String fullName = (u != null) ? u.getFullName() : null;
                String phone    = (u != null) ? u.getPhone()    : null;

                return new DesignerResponse(
                        u != null ? u.getId()   : null,
                        d.getHairStudioId(),           // position
                        d.getIdForLogin(),         // idForLogin
                        fullName,                  // fullName
                        phone,                     // phone
                        d.getPosition(),
                        u != null ? String.valueOf(u.getRole()) : null,
                        d.getStatus(),             // status
                        d.getDaysOff(),            // daysOff
                        Collections.emptyList()   // portfolio (bu yerda portfolioni yuklamaymiz)

                );
            }

            case "CUSTOMER" -> {
                var c = base.customer;
                if (c == null) return buildAdminLikeBase(base);
                var u = base.user;

                // Designer name toplash
                String designerName = null;
                if (c.getDesignerId() != null) {
                    var designerDetail = designerRepo.findByIdAndDeletedAtIsNull(c.getDesignerId())
                            .orElse(null);
                    if (designerDetail != null) {
                        var designerUser = userRepo.findActiveById(designerDetail.getUserId())
                                .orElse(null);
                        if (designerUser != null) {
                            designerName = designerUser.getFullName();
                        }
                    }
                }

                StudioBrief studioInfo = null;

                if (c.getStudioId() != null) {
                    // 1) 먼저 studioId 로 스튜디오 디테일 찾기
                    var studio = studioRepo.findByOwnerUserId(c.getStudioId()).orElse(null);

                    if (studio != null) {
                        // 2) 스튜디오의 owner user 가져오기
                        var ownerOpt = userRepo.findActiveById(studio.getUserId());
                        var owner = ownerOpt.orElse(null);

                        String ownerName  = (owner != null ? owner.getFullName() : null); // getOwnerName = fullName
                        String studioPhone = (owner != null ? owner.getPhone() : null);   // getStudioPhone = user.phone
                        UUID ownerUserId = (owner != null ? owner.getId() : studio.getUserId());

                        studioInfo = new StudioBrief(
                                ownerUserId,                // studio pk
                                ownerName,                      // ✅ user.fullName
                                studioPhone,                    // ✅ user.phone
                                studio.getLogoImageUrl(),       // ✅ detail
                                studio.getBannerImageUrl()      // ✅ detail
                        );
                    }
                }

                // 🔥 Oxirgi service name + date
                String lastServiceName = null;
                LocalDate lastVisitDate = null;

                var page1 = PageRequest.of(0, 1);

                var latestList = reservationRepo.findLatestOneForCustomer(
                        c.getId(),                        // CustomerDetail.id ni reservation.customerId ga yozayotgan bo‘lsang – to‘g‘ri
                        ReservationStatus.COMPLETED,      // sendagi "tugagan" status nomiga mos
                        page1
                );

                if (!latestList.isEmpty()) {
                    var lastRes = latestList.get(0);
                    lastVisitDate = lastRes.getReservationDate();

                    // 🔥 Reservation 에는 이제 List<UUID> serviceIds 가 있음
                    var serviceIds = lastRes.getServiceIds();
                    if (serviceIds != null && !serviceIds.isEmpty()) {
                        UUID mainServiceId = serviceIds.get(0);   // 대표 서비스 하나만 사용

                        var svcOpt = studioServiceRepository.findById(mainServiceId);
                        if (svcOpt.isPresent()) {
                            lastServiceName = svcOpt.get().getServiceName();
                        }
                    }
                }


                return new CustomerResponseForMe(
                        c.getId(),
                        u != null ? u.getFullName() : null,
                        u != null ? u.getPhone() : null,
                        u != null ? u.getRole() : null,
                        c.getEmail(),
                        c.getGender(),
                        designerName,
                        c.getBirthdate(),
                        c.getAddress(),
                        c.getProfileImageUrl(),
                        c.getVisitReason(),
                        c.isConsentMarketing(),
                        studioInfo,
                        lastServiceName,
                        lastVisitDate
                );
            }



            default -> {
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
