package com.moden.modenapi.modules.coupon.service;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.modules.coupon.dto.CouponCreateRequest;
import com.moden.modenapi.modules.coupon.dto.CouponResponse;
import com.moden.modenapi.modules.coupon.dto.CouponUpdateRequest;
import com.moden.modenapi.modules.coupon.model.Coupon;
import com.moden.modenapi.modules.coupon.repository.CouponRepository;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

    private final CouponRepository couponRepository;
    private final HairStudioDetailRepository studioDetailRepository;
    private final CustomerDetailRepository customerDetailRepository;

    // --------------------------
    // CREATE
    // --------------------------
    public CouponResponse create(CouponCreateRequest req) {
        if (req.userId() == null || req.studioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studioId va userId majburiy");
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

        LocalDate start = req.startDate() != null ? req.startDate() : LocalDate.now();
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
                .build();

        Coupon saved = couponRepository.save(c);
        return toRes(saved);
    }

    // --------------------------
    // UPDATE (qisman)
    // --------------------------
    public CouponResponse update(UUID id, CouponUpdateRequest req) {
        Coupon e = couponRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kupon topilmadi"));

        if (req.discountRate() != null && req.discountAmount() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "discountRate va discountAmount dan faqat bittasini yangilang");
        }

        if (req.name() != null) e.setName(req.name());
        if (req.discountRate() != null) {
            if (req.discountRate().signum() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "discountRate > 0 bo‘lishi kerak");
            e.setDiscountRate(req.discountRate());
            e.setDiscountAmount(null);
        }
        if (req.discountAmount() != null) {
            if (req.discountAmount().signum() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "discountAmount > 0 bo‘lishi kerak");
            e.setDiscountAmount(req.discountAmount());
            e.setDiscountRate(null);
        }
        if (req.startDate() != null) e.setStartDate(req.startDate());
        if (req.expiryDate() != null) e.setExpiryDate(req.expiryDate());
        if (req.status() != null) e.setStatus(req.status());
        if (req.birthdayCoupon() != null) e.setBirthdayCoupon(req.birthdayCoupon());
        if (req.firstVisitCoupon() != null) e.setFirstVisitCoupon(req.firstVisitCoupon());

        LocalDate start = e.getStartDate() != null ? e.getStartDate() : LocalDate.now();
        LocalDate end   = e.getExpiryDate();
        if (end != null && end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "tugash sanasi boshlanish sanasidan oldin bo‘la olmaydi");
        }

        Coupon saved = couponRepository.save(e);
        return toRes(saved);
    }

    // --------------------------
    // MARK AS USED
    // --------------------------
    public CouponResponse markAsUsed(UUID id) {
        Coupon c = couponRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kupon topilmadi"));
        c.setStatus(CouponStatus.USED);
        Coupon saved = couponRepository.save(c);
        return toRes(saved);
    }

    // --------------------------
    // GET ONE
    // --------------------------
    @Transactional(readOnly = true)
    public CouponResponse get(UUID id) {
        Coupon c = couponRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kupon topilmadi"));
        return toRes(c);
    }

    // --------------------------
    // LIST BY USER / STATUS
    // --------------------------
    @Transactional(readOnly = true)
    public List<CouponResponse> listByUser(UUID userId) {
        return couponRepository.findAllByUserIdAndDeletedAtIsNull(userId)
                .stream().map(this::toRes).toList();
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> listByUserAndStatus(UUID userId, CouponStatus status) {
        List<Coupon> list = (status != null)
                ? couponRepository.findAllByUserIdAndStatusAndDeletedAtIsNull(userId, status)
                : couponRepository.findAllByUserIdAndDeletedAtIsNull(userId);
        return list.stream().map(this::toRes).toList();
    }

    // --------------------------
    // LIST BY STUDIO / STATUS
    // --------------------------
    @Transactional(readOnly = true)
    public List<CouponResponse> listByStudio(UUID studioId) {
        return couponRepository.findAllByStudioIdAndDeletedAtIsNull(studioId)
                .stream().map(this::toRes).toList();
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> listByStudioAndStatus(UUID studioId, CouponStatus status) {
        List<Coupon> list = (status != null)
                ? couponRepository.findAllByStudioIdAndStatusAndDeletedAtIsNull(studioId, status)
                : couponRepository.findAllByStudioIdAndDeletedAtIsNull(studioId);
        return list.stream().map(this::toRes).toList();
    }
    // ---------------------------------------------------------
    // 🎂  A) Bitta STUDIO uchun bugungi tug‘ilgan mijozlarga kupon berish
    // ---------------------------------------------------------
    public void issueBirthdayCouponsForStudio(UUID studioId) {
        LocalDate today = LocalDate.now();

        List<CustomerDetail> birthdays =
                studioDetailRepository.findBirthdayCustomersToday(studioId, today.getMonthValue(), today.getDayOfMonth());
        if (birthdays.isEmpty()) return;

        for (CustomerDetail cd : birthdays) {
            UUID userId = cd.getUserId();

            boolean hasActiveBirthday = couponRepository.findAllByUserIdAndDeletedAtIsNull(userId)
                    .stream()
                    .anyMatch(c -> c.isBirthdayCoupon()
                            && studioId.equals(c.getStudioId())
                            && c.getStatus() == CouponStatus.AVAILABLE
                            && c.getExpiryDate() != null
                            && !c.getExpiryDate().isBefore(today));

            if (hasActiveBirthday) continue;

            CouponCreateRequest req = new CouponCreateRequest(
                    studioId,
                    userId,
                    "🎂 Tug‘ilgan kun — 30% chegirma",
                    BigDecimal.valueOf(30.0),
                    null,
                    today,
                    today.plusDays(30),
                    true,   // birthdayCoupon
                    false   // firstVisitCoupon
            );

            // ✅ Kupon yaratamiz va id ni olamiz
            CouponResponse created = create(req);

            // ✅ CustomerDetail.last_birthday_coupon_id ga yozamiz
            customerDetailRepository.findByUserId(userId).ifPresent(cust -> {
                cust.setLastBirthdayCouponId(created.id());
                customerDetailRepository.save(cust);
            });
            // (xohlasangiz, else’da warning/exception tashlashingiz mumkin)
        }
    }


    // ---------------------------------------------------------
    // 🎂  B) Barcha STUDIO lar uchun har kuni 00:05 da avtomatik
    // ---------------------------------------------------------
    @Scheduled(cron = "0 5 0 * * ?")
    public void scheduledIssueBirthdayCouponsForAllStudios() {
        List<UUID> studioIds = studioDetailRepository.findActiveStudioIds();
        for (UUID studioId : studioIds) {
            issueBirthdayCouponsForStudio(studioId);
        }
    }

    // --------------------------
    // (quyida mapping/helper metodlari va boshqalar)
    // --------------------------

    private CouponResponse toRes(Coupon c) {
        return new CouponResponse(
                c.getId(),
                c.getStudioId(),
                c.getUserId(),
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

    public void issueFirstVisitCoupon(UUID studioId, UUID userId) {
        if (studioId == null || userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studioId and userId are required");
        }

        LocalDate today = LocalDate.now();

        boolean hasActive = couponRepository.findAllByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .anyMatch(c -> c.isFirstVisitCoupon()
                        && studioId.equals(c.getStudioId())
                        && c.getStatus() == CouponStatus.AVAILABLE
                        && c.getExpiryDate() != null
                        && !c.getExpiryDate().isBefore(today));

        if (hasActive) {
            return; // allaqachon bor — hech narsa qilmaymiz
        }

        // create() validatsiyalariga mos bo'lishi uchun create-request tuzamiz
        CouponCreateRequest req = new CouponCreateRequest(
                studioId,
                userId,
                "💈 First Visit — 10% discount",
                BigDecimal.valueOf(10.0), // discountRate
                null,                      // discountAmount = null (exclusivity)
                today,                     // startDate
                today.plusDays(7),         // expiryDate
                false,                     // birthdayCoupon
                true                       // firstVisitCoupon
        );
    }
}
