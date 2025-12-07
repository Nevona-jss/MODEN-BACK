package com.moden.modenapi.modules.studio.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.common.utils.HtmlSanitizerUtil;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import com.moden.modenapi.modules.studio.dto.StudioBirthdayCouponRequest;
import com.moden.modenapi.modules.studio.dto.StudioPrivacyPolicyRequest;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.dto.StudioUpdateReq;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.sql.Timestamp;
import java.util.stream.Collectors;


@Service
@Transactional
@RequiredArgsConstructor
public class HairStudioService extends BaseService<HairStudioDetail> {

    private final HairStudioDetailRepository studioRepository;
    private final UserRepository userRepo;

    @Override
    protected HairStudioDetailRepository getRepository() {
        return studioRepository;
    }


    /**
     * Studio self-update:
     * - businessNo/fullName: read-only (IGNORE), lekin response'da ko'rsatiladi
     * - boshqa maydonlar: optional update
     * - response: har doim to'liq StudioRes
     */
    @Transactional
    public StudioRes updateSelf(UUID userId, StudioUpdateReq req) {
        var page1 = org.springframework.data.domain.PageRequest.of(0, 1);

        HairStudioDetail s = studioRepository
                .findActiveByUserIdOrderByUpdatedDesc(userId, page1)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Studio not found"));

        if (s.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Studio deleted");
        }

        // ✅ Editable (all optional)
        if (req.ownerName() != null) {s.setOwnerName(req.ownerName());}
        if (req.studioPhone()  != null) s.setStudioPhone(req.studioPhone());
        if (req.address()      != null) s.setAddress(req.address());
        if (req.description()  != null) s.setDescription(req.description());

        if (req.logoImageUrl()    != null) s.setLogoImageUrl(req.logoImageUrl());
        if (req.bannerImageUrl()  != null) s.setBannerImageUrl(req.bannerImageUrl());

        if (req.naver()     != null) s.setNaverUrl(req.naver());
        if (req.kakao()     != null) s.setKakaoUrl(req.kakao());

        if (req.latitude()  != null) s.setLatitude(req.latitude());
        if (req.longitude() != null) s.setLongitude(req.longitude());

        s.setUpdatedAt(java.time.Instant.now());
        studioRepository.saveAndFlush(s);

        var owner = userRepo.findById(s.getUserId()).orElse(null);

        return new StudioRes(
                s.getId(), s.getUserId(),
                owner != null ? owner.getFullName() : null,  // read-only, visible in response
                owner != null ? owner.getPhone()    : null,
                s.getIdForLogin(),
                s.getBusinessNo(),                            // read-only, visible in response
                s.getOwnerName(),                             // read-only, visible in response
                s.getStudioPhone(),
                s.getAddress(),
                s.getDescription(),
                s.getLogoImageUrl(),
                s.getBannerImageUrl(),
                s.getNaverUrl(),
                s.getKakaoUrl(),
                s.getLatitude(),
                s.getLongitude(),
                owner != null ? owner.getRole()    : null

                );
    }

    public UUID getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "No auth");

        String idStr = (auth.getPrincipal() instanceof String s) ? s : auth.getName();
        try {
            return UUID.fromString(idStr);
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
        }
    }


    private HairStudioDetail getStudioByUserIdOrThrow(UUID userId) {
        return studioRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Studio not found for userId=" + userId)
                );
    }

     /**
     * 1) Tug'ilgan kun kupon setting
     */
    public void updateBirthdayCouponSettings(UUID userId, StudioBirthdayCouponRequest req) {
        HairStudioDetail studio = getStudioByUserIdOrThrow(userId);

        studio.setBirthdayCouponEnabled(req.birthdayCouponEnabled());
        studio.setBirthdayCouponDescription(req.birthdayCouponDescription());

        studioRepository.save(studio);
    }

    /**
     * 2) 개인정보/보안 안내 HTML
     */
    public void updatePrivacyPolicyHtml(UUID userId, StudioPrivacyPolicyRequest req) {
        HairStudioDetail studio = getStudioByUserIdOrThrow(userId);

        String safeHtml = HtmlSanitizerUtil.sanitizePrivacyHtml(req.privacyPolicyHtml());
        studio.setPrivacyPolicyHtml(safeHtml);

        studioRepository.save(studio);
    }
    // ✏️ privacyPolicyHtml ni yangilash misoli
    public void updatePrivacyPolicyHtml(UUID studioId, String rawHtml) {
        HairStudioDetail studio = studioRepository.findByUserId(studioId)
                .orElseThrow(() -> new IllegalArgumentException("Studio not found"));

        String safeHtml = sanitizePrivacyHtml(rawHtml);
        studio.setPrivacyPolicyHtml(safeHtml);

        studioRepository.save(studio);
    }

    // 🔒 HTML sanitizatsiya metodi – aynan shu yerda bo‘ladi
    private String sanitizePrivacyHtml(String rawHtml) {
        if (rawHtml == null) return null;

        Safelist safelist = Safelist.relaxed()
                .addTags("p", "br", "ul", "ol", "li", "strong", "b", "em", "i", "u", "span", "div")
                .addAttributes("a", "href", "title")
                .addProtocols("a", "href", "http", "https", "mailto");

        return Jsoup.clean(rawHtml, safelist);
    }
}
