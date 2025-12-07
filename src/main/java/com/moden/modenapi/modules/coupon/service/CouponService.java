package com.moden.modenapi.modules.coupon.service;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.modules.coupon.dto.CouponCreateFirstRegister;
import com.moden.modenapi.modules.coupon.dto.CouponCreateRequest;
import com.moden.modenapi.modules.coupon.dto.CouponFirstRegisterRes;
import com.moden.modenapi.modules.coupon.dto.CouponResponse;
import com.moden.modenapi.modules.coupon.dto.CouponUpdateRequest;
import com.moden.modenapi.modules.coupon.model.Coupon;
import com.moden.modenapi.modules.coupon.repository.CouponRepository;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

    private final CouponRepository couponRepository;
    private final HairStudioDetailRepository hairStudioDetailRepository;
    private final DesignerDetailRepository designerDetailRepository;

    // ----------------------------------------------------------------------
    // 1) 일반 쿠폰 생성
    // ----------------------------------------------------------------------
    public CouponResponse createForCurrentUser(UUID userId, CouponCreateRequest req) {
        UUID studioId = resolveStudioIdForUser(userId);

        validateDiscountPair(req.discountRate(), req.discountAmount());

        LocalDate start = (req.startDate() != null) ? req.startDate() : LocalDate.now();
        LocalDate end   = req.expiryDate();
        validateDateRange(start, end);

        Coupon coupon = Coupon.builder()
                .studioId(studioId)
                .name(req.name())
                .discountRate(req.discountRate())
                .discountAmount(req.discountAmount())
                .description(req.description())
                .startDate(start)
                .expiryDate(end)
                .status(CouponStatus.AVAILABLE)
                .build();

        return toResponse(couponRepository.save(coupon));
    }

    // ----------------------------------------------------------------------
    // 2) FIRST VISIT 쿠폰 생성 (고객 회원가입 시 자동 발급)
    // ----------------------------------------------------------------------
    @Transactional
    public CouponFirstRegisterRes createFirstVisitCouponForCustomer(CustomerDetail customerDetail) {

        UUID studioId = customerDetail.getStudioId();
        if (studioId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "해당 고객은 어떤 헤어샵에도 소속되어 있지 않습니다."
            );
        }

        LocalDate today = LocalDate.now();

        CouponCreateFirstRegister req = new CouponCreateFirstRegister(
                studioId,
                "💈 First Visit — 10% discount",
                BigDecimal.valueOf(10.0),
                null,
                "첫 방문 고객 전용 10% 할인 쿠폰",
                today,
                today.plusDays(30)
        );

        Coupon coupon = buildCouponForStudio(studioId, req);

        if (coupon.getCreatedAt() == null) {
            coupon.setCreatedAt(Instant.now());
        }
        coupon.setUpdatedAt(Instant.now());

        Coupon saved = couponRepository.save(coupon);

        return toFirstRegisterRes(saved);
    }

    // ----------------------------------------------------------------------
    // 4) FIRST REGISTER / POLICY 쿠폰 공통 Builder
    // ----------------------------------------------------------------------
    private Coupon buildCouponForStudio(UUID studioId, CouponCreateFirstRegister req) {

        validateDiscountPair(req.discountRate(), req.discountAmount());

        LocalDate start = (req.startDate() != null) ? req.startDate() : LocalDate.now();
        LocalDate end   = req.expiryDate();
        validateDateRange(start, end);

        return Coupon.builder()
                .studioId(studioId)
                .name(req.name())
                .discountRate(req.discountRate())
                .discountAmount(req.discountAmount())
                .description(req.description())
                .startDate(start)
                .expiryDate(end)
                .status(CouponStatus.AVAILABLE)
                .build();
    }

    // ----------------------------------------------------------------------
    // 5) 할인값 검증
    // ----------------------------------------------------------------------
    private void validateDiscountPair(BigDecimal rate, BigDecimal amount) {
        boolean hasRate   = rate   != null && rate.signum() > 0;
        boolean hasAmount = amount != null && amount.signum() > 0;

        if (hasRate && hasAmount) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "할인율과 정액 할인 금액 중 하나만 입력해야 합니다."
            );
        }
        if (!hasRate && !hasAmount) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "할인율 또는 정액 할인 금액 중 하나는 필수입니다."
            );
        }
    }

    // ----------------------------------------------------------------------
    // 6) 날짜 검증
    // ----------------------------------------------------------------------
    private void validateDateRange(LocalDate start, LocalDate end) {
        if (end != null && end.isBefore(start)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "만료일은 시작일보다 앞설 수 없습니다."
            );
        }
    }

    // ----------------------------------------------------------------------
    // 7) UPDATE
    // ----------------------------------------------------------------------
    @Transactional
    public CouponResponse update(UUID id, CouponUpdateRequest req) {

        Coupon entity = couponRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Kupon topilmadi")
                );

        boolean wantsRateUpdate   = (req.discountRate()   != null);
        boolean wantsAmountUpdate = (req.discountAmount() != null);

        if (wantsRateUpdate && wantsAmountUpdate) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "discountRate va discountAmount dan faqat bittasini yangilang"
            );
        }

        if (req.name() != null) {
            entity.setName(req.name());
        }

        if (wantsRateUpdate) {
            if (req.discountRate().signum() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "discountRate > 0 bo‘lishi kerak"
                );
            }
            entity.setDiscountRate(req.discountRate());
            entity.setDiscountAmount(null);
        }

        if (wantsAmountUpdate) {
            if (req.discountAmount().signum() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "discountAmount > 0 bo‘lishi kerak"
                );
            }
            entity.setDiscountAmount(req.discountAmount());
            entity.setDiscountRate(null);
        }

        if (req.startDate() != null) {
            entity.setStartDate(req.startDate());
        }
        if (req.expiryDate() != null) {
            entity.setExpiryDate(req.expiryDate());
        }
        if (req.status() != null) {
            entity.setStatus(req.status());
        }

        LocalDate start = (entity.getStartDate() != null)
                ? entity.getStartDate()
                : LocalDate.now();

        LocalDate end = entity.getExpiryDate();
        validateDateRange(start, end);

        Coupon saved = couponRepository.save(entity);
        return toResponse(saved);
    }

    // ----------------------------------------------------------------------
    // 8) GET ONE
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public CouponResponse get(UUID id) {
        Coupon c = couponRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Kupon topilmadi")
                );
        return toResponse(c);
    }

    // ----------------------------------------------------------------------
    // 9) LIST BY STUDIO (현재 로그인 user → studio 기준)
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<CouponResponse> listByStudioForCurrentUser(UUID userId) {
        UUID studioId = resolveStudioIdForUser(userId);

        return couponRepository.findAllByStudioIdAndDeletedAtIsNull(studioId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ----------------------------------------------------------------------
    // 10) LIST BY STUDIO + STATUS
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<CouponResponse> listByStudioAndStatusForCurrentUser(UUID userId, CouponStatus status) {
        UUID studioId = resolveStudioIdForUser(userId);

        List<Coupon> list = (status != null)
                ? couponRepository.findAllByStudioIdAndStatusAndDeletedAtIsNull(studioId, status)
                : couponRepository.findAllByStudioIdAndDeletedAtIsNull(studioId);

        return list.stream()
                .map(this::toResponse)
                .toList();
    }

    // ----------------------------------------------------------------------
    // 11) LIST FOR CUSTOMER (Coupon.userId 기준 설계라면 사용 / 아니면 CustomerCouponService 사용)
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<CouponResponse> listForCustomer(UUID customerUserId, CouponStatus status) {
        List<Coupon> list = (status != null)
                ? couponRepository.findAllByUserIdAndStatusAndDeletedAtIsNull(customerUserId, status)
                : couponRepository.findAllByUserIdAndDeletedAtIsNull(customerUserId);

        return list.stream()
                .map(this::toResponse)
                .toList();
    }

    // ----------------------------------------------------------------------
    // 12) SOFT DELETE
    // ----------------------------------------------------------------------
    @Transactional
    public void softDelete(UUID id) {
        Coupon entity = couponRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Coupon is already exist or not created"
                        )
                );

        entity.setDeletedAt(Instant.now());
        couponRepository.save(entity);
    }

    // ----------------------------------------------------------------------
    // 공통: userId → studioId 변환
    // ----------------------------------------------------------------------
    private UUID resolveStudioIdForUser(UUID userId) {
        var studioOpt = hairStudioDetailRepository
                .findByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .findFirst();

        if (studioOpt.isPresent()) {
            return userId;   // studio owner 의 userId
        }

        var designerOpt = designerDetailRepository.findByUserIdAndDeletedAtIsNull(userId);
        if (designerOpt.isPresent()) {
            var dd = designerOpt.get();
            UUID studioUserId = dd.getHairStudioId();

            hairStudioDetailRepository.findByUserIdAndDeletedAtIsNull(studioUserId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Studio profili topilmadi"
                            )
                    );

            return studioUserId;
        }

        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Studio profili topilmadi"
        );
    }

    // ----------------------------------------------------------------------
    // MAPPER
    // ----------------------------------------------------------------------
    private CouponResponse toResponse(Coupon c) {
        return new CouponResponse(
                c.getId(),
                c.getStudioId(),
                c.getName(),
                c.getDiscountRate(),
                c.getDiscountAmount(),
                c.getStatus(),
                c.getDescription(),
                c.getStartDate(),
                c.getExpiryDate(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    private CouponFirstRegisterRes toFirstRegisterRes(Coupon c) {
        return new CouponFirstRegisterRes(
                c.getId(),
                c.getStudioId(),
                c.getName(),
                c.getDiscountRate(),
                c.getDiscountAmount(),
                c.getStatus(),
                c.getDescription(),
                c.getStartDate(),
                c.getExpiryDate(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
