package com.moden.modenapi.modules.coupon.service;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.coupon.dto.CouponCreateReq;
import com.moden.modenapi.modules.coupon.dto.CouponRes;
import com.moden.modenapi.modules.coupon.model.Coupon;
import com.moden.modenapi.modules.coupon.repository.CouponRepository;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
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
public class CouponService extends BaseService<Coupon> {

    private final CouponRepository couponRepository;
    private final CustomerDetailRepository userRepository; // ✅ Injected here

    @Override
    protected CouponRepository getRepository() {
        return couponRepository;
    }

    // ----------------------------------------------------------------------
    // 🔹 CREATE
    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------
// 🔹 CREATE
// ----------------------------------------------------------------------
    public CouponRes create(CouponCreateReq req) {
        if (req.userId() == null || req.studioId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UserId and StudioId are required");

        Coupon c = Coupon.builder()
                .studioId(req.studioId())
                .userId(req.userId())
                .name(req.name())
                .discountRate(req.discountRate())       // ✅ BigDecimal
                .discountAmount(req.discountAmount())   // ✅ BigDecimal
                .birthdayCoupon(req.birthdayCoupon())
                .firstVisitCoupon(req.firstVisitCoupon())
                .expiryDate(req.expiryDate())
                .status(CouponStatus.AVAILABLE)
                .build();

        create(c);
        return toRes(c);
    }


    // ----------------------------------------------------------------------
    // 🔹 MARK AS USED
    // ----------------------------------------------------------------------
    public CouponRes markAsUsed(UUID id) {
        Coupon c = couponRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));
        c.setStatus(CouponStatus.USED);
        c.setUpdatedAt(Instant.now());
        update(c);
        return toRes(c);
    }

    // ----------------------------------------------------------------------
    // 🔹 USER COUPON LIST
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<CouponRes> listByUser(UUID userId) {
        return couponRepository.findAllByUserIdAndDeletedAtIsNull(userId)
                .stream().map(this::toRes).toList();
    }

    @Transactional(readOnly = true)
    public List<CouponRes> listByUserAndStatus(UUID userId, CouponStatus status) {
        List<Coupon> coupons = (status != null)
                ? couponRepository.findAllByUserIdAndStatusAndDeletedAtIsNull(userId, status)
                : couponRepository.findAllByUserIdAndDeletedAtIsNull(userId);

        return coupons.stream().map(this::toRes).toList();
    }

    // ----------------------------------------------------------------------
    // 🔹 MAPPER
    // ----------------------------------------------------------------------
    private CouponRes toRes(Coupon c) {
        return new CouponRes(
                c.getId(),
                c.getStudioId(),
                c.getUserId(),
                c.getName(),
                c.getDiscountRate(),
                c.getDiscountAmount(),
                c.getStatus(),
                c.isBirthdayCoupon(),
                c.isFirstVisitCoupon(),
                c.getExpiryDate(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    // ----------------------------------------------------------------------
    // ⚙️ 매일 자정(00:00) 만료 쿠폰 자동 처리
    // ----------------------------------------------------------------------
    @Scheduled(cron = "0 0 0 * * ?") // Every day at midnight
    @Transactional
    public void autoExpireCoupons() {
        List<Coupon> coupons = couponRepository.findAllByDeletedAtIsNull();
        LocalDate today = LocalDate.now();

        coupons.stream()
                .filter(c -> c.getExpiryDate() != null
                        && c.getExpiryDate().isBefore(today)
                        && c.getStatus() == CouponStatus.AVAILABLE)
                .forEach(c -> {
                    c.setStatus(CouponStatus.EXPIRED);
                    update(c);
                });

        System.out.println("✅ [Scheduler] Expired coupons updated at " + Instant.now());
    }

    // ----------------------------------------------------------------------
    // 🎂 매일 자정: 생일 쿠폰 자동 발급
    @Scheduled(cron = "0 0 0 * * ?") // Every midnight
    @Transactional
    public void autoIssueBirthdayCoupons() {
        LocalDate today = LocalDate.now();
        List<CustomerDetail> birthdayUsers = userRepository.findAllByBirthDateMonthAndDay(
                today.getMonthValue(),
                today.getDayOfMonth()
        );

        System.out.println("🎂 [Scheduler] Found " + birthdayUsers.size() + " users with birthdays today");

        for (CustomerDetail user : birthdayUsers) {
            UUID userId = user.getUserId();

            // 🔹 Find studioId from user's latest coupon
            UUID studioId = couponRepository.findAllByUserIdAndDeletedAtIsNull(userId)
                    .stream()
                    .filter(c -> c.getStudioId() != null)
                    .map(Coupon::getStudioId)
                    .findFirst()
                    .orElse(null);

            if (studioId == null) {
                System.out.println("⚠️ [Scheduler] Skipped user " + userId + " — no existing studioId found in coupons");
                continue;
            }

            // 🔹 Check if user already has an active birthday coupon
            boolean hasActive = couponRepository.findAllByUserIdAndDeletedAtIsNull(userId)
                    .stream()
                    .anyMatch(c -> c.isBirthdayCoupon()
                            && c.getStudioId().equals(studioId)
                            && c.getStatus() == CouponStatus.AVAILABLE
                            && c.getExpiryDate() != null
                            && !c.getExpiryDate().isBefore(today));

            if (hasActive) {
                System.out.println("ℹ️ [Scheduler] User " + userId + " already has an active birthday coupon");
                continue;
            }

            // ✅ Build coupon request
            CouponCreateReq req = new CouponCreateReq(
                    studioId,
                    userId,
                    "🎂 생일 특별 30% 할인 쿠폰",
                    BigDecimal.valueOf(30.0),   // ✅ correct type
                    BigDecimal.ZERO,            // ✅ cleaner than valueOf(0.0)
                    true,
                    false,
                    today.plusDays(30)
            );

            // ✅ Reuse existing create() method
            create(req);

            System.out.println("🎁 [Scheduler] Birthday coupon issued for user " + userId + " (studio: " + studioId + ")");
        }
    }
    @Transactional
    public void issueFirstVisitCoupon(UUID studioId, UUID userId) {
        if (studioId == null || userId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studioId and userId are required");

        LocalDate today = LocalDate.now();

        // 🔹 Avoid duplicates
        boolean hasActive = couponRepository.findAllByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .anyMatch(c -> c.isFirstVisitCoupon()
                        && c.getStudioId().equals(studioId)
                        && c.getStatus() == CouponStatus.AVAILABLE
                        && c.getExpiryDate() != null
                        && !c.getExpiryDate().isBefore(today));

        if (hasActive) {
            System.out.println("ℹ️ User already has a first-visit coupon for this studio.");
            return;
        }

        // ✅ Build Coupon Request
        CouponCreateReq req = new CouponCreateReq(
                studioId,
                userId,
                "💈 첫 방문 특별 10% 할인 쿠폰", // or "First Visit Special 10% Discount Coupon"
                BigDecimal.valueOf(10.0),   // ✅ correct type
                BigDecimal.ZERO,            // ✅ cleaner than valueOf(0.0)
                false,    // birthdayCoupon
                true,     // firstVisitCoupon
                today.plusDays(7) // valid for 7 days
        );

        create(req); // reuse your existing method
        System.out.println("🎁 First-visit coupon issued for user " + userId + " (studio: " + studioId + ")");
    }



}
