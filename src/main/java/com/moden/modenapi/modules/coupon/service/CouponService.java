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
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    // 1) 일반 쿠폰 생성 (쿠폰 생성 화면에서 /coupons/create 호출)
    // ----------------------------------------------------------------------
    public CouponResponse createForCurrentUser(UUID userId, CouponCreateRequest req) {
        UUID studioId = resolveStudioIdForUser(userId);

        // 할인값 검증 (rate/amount pair rule 재사용)
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
//    studioId는 항상 CustomerDetail.studioId 기준
// ----------------------------------------------------------------------
    public CouponFirstRegisterRes createFirstVisitCouponForCustomer(CustomerDetail customerDetail) {

        // 1) studioId 를 customerDetail 에서 가져오기
        UUID studioId = customerDetail.getStudioId();
        if (studioId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "해당 고객은 어떤 헤어샵에도 소속되어 있지 않습니다."
            );
        }

        // 2) FIRST VISIT 전용 기본값 구성
        LocalDate today = LocalDate.now();

        CouponCreateFirstRegister req = new CouponCreateFirstRegister(
                studioId,
                "💈 First Visit — 10% discount", // name
                BigDecimal.valueOf(10.0),        // discountRate (10%)
                null,                            // discountAmount (정율이므로 null)
                "첫 방문 고객 전용 10% 할인 쿠폰",    // description
                today,                           // startDate
                today.plusDays(30)               // expiryDate
        );

        // 3) 공통 빌더 사용해서 Coupon 엔티티 생성
        Coupon coupon = buildCouponForStudio(studioId, req);

        // 필요하면 FIRST VISIT 전용 플래그들 설정 (엔티티에 필드 있을 경우)
        // coupon.setFirstVisitCoupon(true);
        // coupon.setBirthdayCoupon(false);
        // coupon.setGlobal(false);

        // 생성·수정 시각 기본값
        if (coupon.getCreatedAt() == null) {
            coupon.setCreatedAt(Instant.now());
        }
        coupon.setUpdatedAt(Instant.now());

        Coupon saved = couponRepository.save(coupon);

        // 4) FirstRegister용 응답 DTO로 매핑
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
    // 5) 할인값 검증 (rate/amount 중 하나만, 최소 0보다 큰 값)
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
    // 6) 날짜 검증 (end before start 불가)
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
    // 7) UPDATE (qisman policy)
    // ----------------------------------------------------------------------
    @Transactional
    public CouponResponse update(UUID id, CouponUpdateRequest req) {

        // 1) Avval DB dan kuponni olib kelamiz (faqat o‘chirilmagan bo‘lsa)
        Coupon entity = couponRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Kupon topilmadi")
                );

        // 2) Chegirma miqdori bo‘yicha validatsiya
        boolean wantsRateUpdate   = (req.discountRate()   != null);
        boolean wantsAmountUpdate = (req.discountAmount() != null);

        if (wantsRateUpdate && wantsAmountUpdate) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "discountRate va discountAmount dan faqat bittasini yangilang"
            );
        }

        // 3) Oddiy field’larni patch qilish (null bo‘lmaganlarini)
        if (req.name() != null) {
            entity.setName(req.name());
        }

        // 4) discountRate yangilanayotgan bo‘lsa
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

        // 5) discountAmount yangilanayotgan bo‘lsa
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

        // 6) Sana, status
        if (req.startDate() != null) {
            entity.setStartDate(req.startDate());
        }
        if (req.expiryDate() != null) {
            entity.setExpiryDate(req.expiryDate());
        }
        if (req.status() != null) {
            entity.setStatus(req.status());
        }

        // 7) Sana mantiqiyligini tekshiramiz
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
    // 10) LIST BY STUDIO + STATUS (현재 로그인 user → studio 기준)
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
    // 11) LIST FOR CUSTOMER (고객 userId 기준으로 보유 쿠폰 조회)
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
        // 필요하면 상태도 같이 변경
        // entity.setStatus(CouponStatus.EXPIRED);

        couponRepository.save(entity);
    }

    // ----------------------------------------------------------------------
    // 공통: userId → studioId 변환
    // ----------------------------------------------------------------------
    private UUID resolveStudioIdForUser(UUID userId) {
        // 1) 먼저: 이 userId 로 등록된 스튜디오(owner) 가 있는지 체크
        var studioOpt = hairStudioDetailRepository
                .findByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .findFirst();

        if (studioOpt.isPresent()) {
            // ✅ 비즈니스에서 쓰는 studioId = studio owner 의 userId
            return userId;
        }

        // 2) 없으면: 디자이너인지 확인
        var designerOpt = designerDetailRepository.findByUserIdAndDeletedAtIsNull(userId);
        if (designerOpt.isPresent()) {
            var dd = designerOpt.get();

            // ✅ 여기서도 dd.getHairStudioId() 는 "스튜디오 userId" 라고 약속
            UUID studioUserId = dd.getHairStudioId();

            // 원하면 검증만 한 번:
            hairStudioDetailRepository.findByUserIdAndDeletedAtIsNull(studioUserId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Studio profili topilmadi"
                            )
                    );

            return studioUserId;  // ✅ 비즈니스 studioId = studioUserId
        }

        // 3) 둘 다 아니면 studio profile 없음
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
