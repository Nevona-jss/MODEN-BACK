package com.moden.modenapi.modules.coupon.service;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.modules.coupon.dto.CouponCreateRequest;
import com.moden.modenapi.modules.coupon.dto.CouponResponse;
import com.moden.modenapi.modules.coupon.dto.CouponUpdateRequest;
import com.moden.modenapi.modules.coupon.model.Coupon;
import com.moden.modenapi.modules.coupon.repository.CouponRepository;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

    private final CouponRepository couponRepository;
    private final HairStudioDetailRepository hairStudioDetailRepository;

    // --------------------------
    // CREATE  (faqat policy)
    // --------------------------
    public CouponResponse create(CouponCreateRequest req) {
        if (req.studioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studioId majburiy");
        }

        boolean hasRate   = req.discountRate()   != null && req.discountRate().signum() > 0;
        boolean hasAmount = req.discountAmount() != null && req.discountAmount().signum() > 0;

        if (hasRate && hasAmount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "discountRate va discountAmount dan faqat bittasini kiriting");
        }
        if (!hasRate && !hasAmount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "discountRate yoki discountAmount dan bittasi majburiy");
        }

        LocalDate start = (req.startDate() != null) ? req.startDate() : LocalDate.now();
        LocalDate end   = req.expiryDate();
        if (end != null && end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "tugash sanasi boshlanish sanasidan oldin bo‘la olmaydi");
        }

        Coupon c = Coupon.builder()
                .studioId(req.studioId())
                .userId(req.userId())
                .name(req.name())
                .discountRate(hasRate ? req.discountRate() : null)
                .discountAmount(hasAmount ? req.discountAmount() : null)
                .startDate(start)
                .expiryDate(end)
                .birthdayCoupon(req.birthdayCoupon())
                .firstVisitCoupon(req.firstVisitCoupon())
                .status(CouponStatus.AVAILABLE)
                .isGlobal(req.isGlobal())
                .build();


        return toRes(couponRepository.save(c));
    }

    // --------------------------
    // UPDATE (qisman policy)
    // --------------------------
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

        // Ikkalasini ham bir vaqtning o‘zida yangilashga ruxsat bermaymiz
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
            entity.setDiscountAmount(null);  // rate ishlatsa, amount ni tozalaymiz
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
            entity.setDiscountRate(null);    // amount ishlatsa, rate ni tozalaymiz
        }

        // 6) Sana, status, flag’lar (null bo‘lmasa yangilaymiz)
        if (req.startDate() != null) {
            entity.setStartDate(req.startDate());
        }
        if (req.expiryDate() != null) {
            entity.setExpiryDate(req.expiryDate());
        }
        if (req.status() != null) {
            entity.setStatus(req.status());
        }
        if (req.birthdayCoupon() != null) {
            entity.setBirthdayCoupon(req.birthdayCoupon());
        }
        if (req.firstVisitCoupon() != null) {
            entity.setFirstVisitCoupon(req.firstVisitCoupon());
        }
        // agar CouponUpdateRequest da isGlobal ham bo‘lsa:
        // if (req.isGlobal() != null) {
        //     entity.setGlobal(req.isGlobal());
        // }

        // 7) Sana mantiqiyligini tekshiramiz
        LocalDate start = (entity.getStartDate() != null)
                ? entity.getStartDate()
                : LocalDate.now();    // startDate null bo‘lsa, bugungi sana sifatida qabul qilamiz

        LocalDate end = entity.getExpiryDate();
        if (end != null && end.isBefore(start)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "tugash sanasi boshlanish sanasidan oldin bo‘la olmaydi"
            );
        }

        // 8) DB ga saqlaymiz va eng oxirgi holatini qaytaramiz
        Coupon saved = couponRepository.save(entity);

        // 9) DTO ga map qilib, Controllerga yuboramiz
        return toRes(saved);
    }


    // --------------------------
    // GET ONE (policy)
    // --------------------------
    @Transactional(readOnly = true)
    public CouponResponse get(UUID id) {
        Coupon c = couponRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kupon topilmadi"));
        return toRes(c);
    }

    // --------------------------
    // LIST BY STUDIO / STATUS (policy)
    // --------------------------

    @Transactional(readOnly = true)
    public List<CouponResponse> listByStudioForCurrentUser(UUID userId) {
        // 1) 먼저 userId → studioId 찾기
        var page1 = PageRequest.of(0, 1);
        var studioOpt = hairStudioDetailRepository
                .findActiveByUserIdOrderByUpdatedDesc(userId, page1)
                .stream()
                .findFirst();

        if (studioOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Studio profili topilmadi");
        }

        HairStudioDetail studio = studioOpt.get();
        UUID studioId = studio.getId();  // ✅ 쿠폰의 studioId랑 같은 값

        // 2) 해당 studioId 기준으로 쿠폰 조회 (정렬도 추가하고 싶으면 OrderBy...)
        return couponRepository.findAllByStudioIdAndDeletedAtIsNull(studioId)
                .stream()
                .map(this::toRes)
                .toList();
    }

    // 공통: userId → studioId 변환
    private UUID resolveStudioIdForUser(UUID userId) {
        var page1 = PageRequest.of(0, 1);

        var studioOpt = hairStudioDetailRepository
                .findActiveByUserIdOrderByUpdatedDesc(userId, page1)
                .stream()
                .findFirst();

        if (studioOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Studio profili topilmadi");
        }

        HairStudioDetail studio = studioOpt.get();
        return studio.getId();  // 👉 bu Coupon.studioId bilan bir xil bo‘lishi kerak
    }

    // --------------------------
    // LIST BY STUDIO + STATUS (userId asosida)
    // --------------------------
    @Transactional(readOnly = true)
    public List<CouponResponse> listByStudioAndStatusForCurrentUser(UUID userId, CouponStatus status) {
        UUID studioId = resolveStudioIdForUser(userId);  // userId → studioId

        List<Coupon> list = (status != null)
                ? couponRepository.findAllByStudioIdAndStatusAndDeletedAtIsNull(studioId, status)
                : couponRepository.findAllByStudioIdAndDeletedAtIsNull(studioId);

        return list.stream()
                .map(this::toRes)
                .toList();
    }

    // --------------------------
    // MAPPER
    // --------------------------
    private CouponResponse toRes(Coupon c) {
        return new CouponResponse(
                c.getId(),
                c.getStudioId(),
                null,                // 🔴 userId endi yo‘q → null qaytaramiz (DTO’ni keyin customerId ga moslab alohida tuzishingiz mumkin)
                c.getName(),
                c.getDiscountRate(),
                c.getDiscountAmount(),
                c.getStatus(),
                c.getStartDate(),
                c.getExpiryDate(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.isBirthdayCoupon(),
                c.isFirstVisitCoupon()
        );
    }
}
