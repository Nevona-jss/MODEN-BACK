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
     * 한 명의 customer 에게 쿠폰 1개 발급
     * - studio/coupon/customer 유효성 체크
     * - 이미 가지고 있는 쿠폰인지 중복 체크
     * - 이상 없으면 CustomerCoupon 저장
     */
    @Transactional
    public void assignToCustomer(UUID studioId, UUID couponId, UUID customerId) {

        // 1) 쿠폰 존재 + 삭제 안된 것
        Coupon coupon = couponRepository.findByIdAndDeletedAtIsNull(couponId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Coupon topilmadi (id=" + couponId + ")"
                ));

        // 2) 쿠폰이 해당 studio 의 것인지
        if (!coupon.getStudioId().equals(studioId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Coupon ushbu studionga tegishli emas"
            );
        }

        // 3) customer 존재 + studio 매칭 확인
        CustomerDetail customer = customerDetailRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer topilmadi (id=" + customerId + ")"
                ));

        if (!customer.getStudioId().equals(studioId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Customer ushbu studionga tegishli emas"
            );
        }

        // 4) 이미 이 쿠폰을 가진 상태인지? (AVAILABLE / EXPIRED / USED 모두 포함)
        boolean alreadyHas = customerCouponRepository.existsByCouponIdAndCustomerIdAndStatusIn(
                couponId,
                customerId,
                List.of(CouponStatus.AVAILABLE, CouponStatus.EXPIRED, CouponStatus.USED)
        );

        if (alreadyHas) {
            // 이미 발급된 경우는 에러로 던져서 scheduler 에서 catch 해서 skip 하도록
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Customer already has this coupon"
            );
        }

        // 5) CustomerCoupon 저장 (여기서 studioId 는 customer 에서 가져온 값과 동일)
        CustomerCoupon cc = CustomerCoupon.builder()
                .studioId(studioId)
                .couponId(couponId)
                .customerId(customerId)
                .status(CouponStatus.AVAILABLE)
                .issuedAt(Instant.now())
                .build();

        customerCouponRepository.save(cc);
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
                coupon != null ? coupon.getName() : null,
                coupon != null ? coupon.getDiscountRate() : null,
                coupon != null ? coupon.getDiscountAmount() : null,
                cc.getStatus(),
                coupon != null ? coupon.getStartDate() : null,
                coupon != null ? coupon.getExpiryDate() : null,
                cc.getIssuedAt(),
                cc.getUsedAt()
        );
    }


    @Transactional(readOnly = true)
    public List<CustomerCouponRes> listCouponsForCustomerUser(UUID customerUserId) {
        // 1) userId → CustomerDetail topish (latest record)
        var page1 = PageRequest.of(0, 1);

        var customerOpt = customerDetailRepository
                .findActiveByUserIdOrderByUpdatedDesc(customerUserId, page1)
                .stream()
                .findFirst();

        if (customerOpt.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Customer not found"
            );
        }

        CustomerDetail customer = customerOpt.get();

        // 2) CustomerDetail.id (customerId) bo'yicha kuponlar (status=null → hammasi)
        return getCouponsForCustomer(customer.getId(), null);
    }


}