package com.moden.modenapi.common.utils;

import com.moden.modenapi.modules.coupon.model.Coupon;
import com.moden.modenapi.modules.coupon.service.CustomerCouponService;
import com.moden.modenapi.modules.coupon.repository.CouponRepository;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BirthdayCouponScheduler {

    private final CustomerDetailRepository customerDetailRepository;
    private final CouponRepository couponRepository;
    private final CustomerCouponService customerCouponService;

    /**
     * Har kuni soat 09:00 da (Asia/Tashkent 기준)
     *  1) bugun tug'ilgan barcha customerlarni topadi
     *  2) har bir customer uchun studioId + customerId ni avtomatik oladi
     *  3) o'sha studioning active birthday couponini topadi
     *  4) customer_coupon ga yozib qo'yadi (assignToCustomer)
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Tashkent")
    public void distributeBirthdayCoupons() {

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Tashkent"));
        int month = today.getMonthValue();
        int day   = today.getDayOfMonth();

        Instant now = Instant.now();

        // 1) bugun tug'ilgan barcha customerlar
        List<CustomerDetail> birthdayCustomers =
                customerDetailRepository.findAllByBirthday(month, day);

        log.info("Birthday check - today={}, birthdayCustomers={}", today, birthdayCustomers.size());

        for (CustomerDetail customer : birthdayCustomers) {
            UUID customerId = customer.getId();        // ✅ 자동 customer ID
            UUID studioId   = customer.getStudioId();  // ✅ 자동 studio ID

            // 2) ushbu studioning birthday couponini topamiz
            Optional<Coupon> birthdayCouponOpt =
                    couponRepository.findActiveBirthdayCouponForStudio(studioId, today);

            if (birthdayCouponOpt.isEmpty()) {
                log.info("No active birthday coupon for studioId={} (skip customerId={})",
                        studioId, customerId);
                continue;
            }

            Coupon birthdayCoupon = birthdayCouponOpt.get();

            try {
                // 3) mavjud bo'lmasa yozib qo'yish (중복 체크는 assignToCustomer 안에 있음)
                customerCouponService.assignToCustomer(studioId, birthdayCoupon.getId(), customerId);

                log.info("Birthday coupon assigned. studioId={}, customerId={}, couponId={}",
                        studioId, customerId, birthdayCoupon.getId());
            } catch (ResponseStatusException ex) {
                // 이미 받은 경우 등
                log.info("Skip birthday coupon. studioId={}, customerId={}, reason={}",
                        studioId, customerId, ex.getReason());
            } catch (Exception ex) {
                log.error("Error assigning birthday coupon. studioId={}, customerId={}",
                        studioId, customerId, ex);
            }
        }
    }
}
