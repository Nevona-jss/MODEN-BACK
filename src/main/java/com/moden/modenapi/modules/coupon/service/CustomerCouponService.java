package com.moden.modenapi.modules.coupon.service;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.modules.coupon.dto.CustomerCouponRes;
import com.moden.modenapi.modules.coupon.model.Coupon;
import com.moden.modenapi.modules.coupon.model.CustomerCoupon;
import com.moden.modenapi.modules.coupon.repository.CouponRepository;
import com.moden.modenapi.modules.coupon.repository.CustomerCouponRepository;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerCouponService {

    private final CustomerCouponRepository customerCouponRepository;
    private final CouponRepository couponRepository;
    private final CustomerDetailRepository customerDetailRepository;



    /**
     * Hozir login bo'lgan USER (userId) uchun,
     * unga bog'langan CUSTOMER profilini topib,
     * shu customerni kuponlarini qaytaradi.
     */
    @Transactional(readOnly = true)
    public List<CustomerCouponRes> getCouponsForCurrentCustomerUser(UUID userId, CouponStatus status) {
        // CustomerDetailRepository da 이미 이런 메서드 있음:
        // findActiveByUserIdOrderByUpdatedDesc(userId, pageRequest)
        var page1 = PageRequest.of(0, 1);

        var customerOpt = customerDetailRepository
                .findActiveByUserIdOrderByUpdatedDesc(userId, page1)
                .stream()
                .findFirst();

        if (customerOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profili topilmadi");
        }

        UUID customerId = customerOpt.get().getId();  // 🔹 bu asl customer_coupon.customer_id ga mos keladigan ID

        return getCouponsForCustomer(customerId, status);
    }
    /**
     * Berilgan studioda, bugun tug'ilgan kuni bo'lgan customerlarga
     * birthday coupon berish.
     */
    public int assignBirthdayCouponsForToday(UUID studioId, UUID birthdayCouponId) {

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Tashkent"));
        int month = today.getMonthValue();
        int day   = today.getDayOfMonth();

        // 1) Kupon mavjudmi va studiogami?
        Coupon coupon = couponRepository.findByIdAndDeletedAtIsNull(birthdayCouponId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Birthday coupon topilmadi"));

        if (!coupon.getStudioId().equals(studioId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Birthday coupon ushbu studionga tegishli emas");
        }

        // 2) Bugun tug'ilgan kundagi customerlar
        List<CustomerDetail> birthdayCustomers =
                customerDetailRepository.findBirthdayCustomersOnDate(studioId, month, day);

        int createdCount = 0;

        for (CustomerDetail customer : birthdayCustomers) {
            UUID customerId = customer.getId();

            // 3) Allaqachon shu birthday kupon bor-yo'qligini tekshirish
            boolean alreadyHas = customerCouponRepository.existsByCouponIdAndCustomerIdAndStatusIn(
                    birthdayCouponId,
                    customerId,
                    List.of(CouponStatus.AVAILABLE, CouponStatus.EXPIRED, CouponStatus.USED)
            );
            if (alreadyHas) {
                continue;
            }

            // 4) Yangi customer_coupon yozamiz
            CustomerCoupon cc = CustomerCoupon.builder()
                    .studioId(studioId)
                    .couponId(birthdayCouponId)
                    .customerId(customerId)
                    .status(CouponStatus.AVAILABLE)
                    .issuedAt(Instant.now())
                    .build();

            customerCouponRepository.save(cc);
            createdCount++;
        }

        return createdCount;
    }

    /**
     * Kuponni biror customerga berish (customer_coupon ga yozish)
     */
    public CustomerCoupon assignToCustomer(UUID studioId, UUID couponId, UUID customerId) {

        // 1) Kupon mavjudmi va o‘sha studiogami
        Coupon coupon = couponRepository.findByIdAndDeletedAtIsNull(couponId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));

        if (!coupon.getStudioId().equals(studioId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon ushbu studionga tegishli emas");
        }

        // 2) Shu customer + shu coupon bo‘yicha allaqachon yozuv bormi?
        boolean alreadyHas = customerCouponRepository.existsByCouponIdAndCustomerIdAndStatusIn(
                couponId,
                customerId,
                List.of(CouponStatus.AVAILABLE, CouponStatus.EXPIRED, CouponStatus.USED)
        );
        if (alreadyHas) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer allaqachon bu kuponni olgan");
        }

        CustomerCoupon cc = CustomerCoupon.builder()
                .studioId(studioId)
                .couponId(couponId)
                .customerId(customerId)
                .status(CouponStatus.AVAILABLE)
                .issuedAt(Instant.now())
                .build();

        return customerCouponRepository.save(cc);
    }

    /**
     * Customer kuponni ishlatadi (faqat status EXPIRED qilinadi)
     */
    public CustomerCoupon useCustomerCoupon(UUID customerCouponId, UUID customerId) {
        CustomerCoupon cc = customerCouponRepository.lockByIdForUpdate(customerCouponId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer coupon topilmadi"));

        // Kupon shu customerniki bo‘lishi kerak
        if (!cc.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu kupon ushbu customerniki emas");
        }

        // status AVAILABLE bo‘lishi shart
        if (cc.getStatus() != CouponStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kupon available emas");
        }

        // ishlatilgan — endi EXPIRED
        cc.setStatus(CouponStatus.EXPIRED);
        cc.setUsedAt(Instant.now());

        return customerCouponRepository.save(cc);
    }

    // ----------------------------------------------------------------------
    // CUSTOMER: o'z kuponlarini ko‘rishi (DTO)
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<CustomerCouponRes> getCouponsForCustomer(UUID customerId, CouponStatus status) {

        List<CustomerCoupon> list;

        if (status != null) {
            list = customerCouponRepository
                    .findAllByCustomerIdAndStatusAndDeletedAtIsNull(customerId, status);
        } else {
            list = customerCouponRepository
                    .findAllByCustomerIdAndDeletedAtIsNull(customerId);
        }

        return list.stream()
                .map(this::toCustomerCouponRes)
                .toList();
    }

    private CustomerCouponRes toCustomerCouponRes(CustomerCoupon cc) {
        // CustomerCoupon ↔ Coupon faqat ID orqali bog'langan, shuning uchun repo dan olib kelamiz
        Coupon coupon = couponRepository.findByIdAndDeletedAtIsNull(cc.getCouponId())
                .orElse(null);  // kupon o'chib ketgan bo'lsa null bo'lishi mumkin

        return new CustomerCouponRes(
                cc.getId(),
                cc.getStudioId(),
                cc.getCouponId(),
                coupon != null ? coupon.getName()           : null,
                coupon != null ? coupon.getDiscountRate()   : null,
                coupon != null ? coupon.getDiscountAmount() : null,
                cc.getStatus(),
                coupon != null ? coupon.getStartDate()      : null,
                coupon != null ? coupon.getExpiryDate()     : null,
                cc.getIssuedAt(),
                cc.getUsedAt()
        );
    }

}
