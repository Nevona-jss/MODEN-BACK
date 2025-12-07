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

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerCouponService {

    private final CustomerCouponRepository customerCouponRepository;
    private final CouponRepository couponRepository;
    private final CustomerDetailRepository customerDetailRepository;

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Tashkent");

    // ----------------------------------------------------------------------
    // 1) 현재 로그인한 USER 기준으로 customer coupon + filter
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<CustomerCouponRes> getCouponsForCurrentCustomerUser(
            UUID userId,
            CouponStatus status,
            String period,
            List<String> serviceNames
    ) {
        UUID customerId = resolveCustomerIdForUser(userId);   // 👉 여기서 userId 그대로 리턴
        return getCouponsForCustomer(customerId, status, period, serviceNames);
    }

    // ----------------------------------------------------------------------
    // 2) 특정 customerId 기준 filter (status + period + serviceNames)
    //    여기서의 customerId = 고객 User.id
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<CustomerCouponRes> getCouponsForCustomer(
            UUID customerId,
            CouponStatus status,
            String period,
            List<String> serviceNames
    ) {
        // 1) customerId(=userId) bo‘yicha barcha CustomerCoupon
        List<CustomerCoupon> base = customerCouponRepository
                .findAllByCustomerIdAndDeletedAtIsNullOrderByCreatedAtDesc(customerId);

        // 2) period → from Instant
        Instant from = resolveFromForPeriod(period);

        // 3) serviceNames filter set
        Set<String> serviceNameFilter = null;
        if (serviceNames != null && !serviceNames.isEmpty()) {
            serviceNameFilter = serviceNames.stream()
                    .filter(Objects::nonNull)
                    .map(s -> s.trim().toLowerCase())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            if (serviceNameFilter.isEmpty()) {
                serviceNameFilter = null;
            }
        }

        Set<String> finalServiceNameFilter = serviceNameFilter;
        CouponStatus finalStatus = status;

        return base.stream()
                .filter(cc -> {
                    // ⏰ period filter: createdAt 기준
                    if (from != null) {
                        Instant created = cc.getCreatedAt();
                        if (created == null || created.isBefore(from)) {
                            return false;
                        }
                    }

                    // 🔹 Coupon 로드 (status & name filter)
                    Coupon coupon = couponRepository
                            .findByIdAndDeletedAtIsNull(cc.getCouponId())
                            .orElse(null);
                    if (coupon == null) return false;

                    // status filter
                    if (finalStatus != null && coupon.getStatus() != finalStatus) {
                        return false;
                    }

                    // serviceName filter: Coupon.name
                    if (finalServiceNameFilter != null) {
                        String couponName = coupon.getName();
                        if (couponName == null) return false;

                        if (!finalServiceNameFilter.contains(couponName.trim().toLowerCase())) {
                            return false;
                        }
                    }

                    return true;
                })
                .map(this::toCustomerCouponRes)
                .toList();
    }

    // ----------------------------------------------------------------------
    // 3) 특정 customerId 기준, status 만으로 필터 (단순 목록)
    //    여기서의 customerId = 고객 User.id
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<CustomerCouponRes> getCouponsForCustomer(UUID customerId, CouponStatus status) {

        List<CustomerCoupon> list = customerCouponRepository
                .findAllByCustomerIdAndDeletedAtIsNullOrderByCreatedAtDesc(customerId);

        return list.stream()
                .filter(cc -> {
                    if (status == null) return true;
                    Coupon coupon = couponRepository
                            .findByIdAndDeletedAtIsNull(cc.getCouponId())
                            .orElse(null);
                    return coupon != null && coupon.getStatus() == status;
                })
                .map(this::toCustomerCouponRes)
                .toList();
    }

    // ----------------------------------------------------------------------
    // 4) Studio → 특정 customer (userId) 쿠폰 list (controller에서 사용)
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<CustomerCouponRes> listCouponsForCustomerUser(UUID customerUserId) {
        UUID customerId = resolveCustomerIdForUser(customerUserId);  // = userId
        return getCouponsForCustomer(customerId, null);
    }

    // ----------------------------------------------------------------------
    // 5) CUSTOMER coupon assign (studio가 고객에게 쿠폰 발급)
    //    customerId = 고객 User.id
    // ----------------------------------------------------------------------
    @Transactional
    public void assignToCustomer(UUID studioId, UUID couponId, UUID customerUserId) {

        // 1) 쿠폰 존재 + soft delete 아님
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
        CustomerDetail customer = customerDetailRepository
                .findActiveByUserIdOrderByUpdatedDesc(customerUserId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer topilmadi (userId=" + customerUserId + ")"
                ));

        if (!customer.getStudioId().equals(studioId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Customer ushbu studionga tegishli emas"
            );
        }

        // 4) 이미 동일 couponId를 가진 CustomerCoupon 이 있는지
        boolean alreadyHas = customerCouponRepository
                .existsByCouponIdAndCustomerIdAndDeletedAtIsNull(couponId, customerUserId);

        if (alreadyHas) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Customer already has this coupon"
            );
        }

        // 5) CustomerCoupon 저장
        CustomerCoupon cc = CustomerCoupon.builder()
                .studioId(studioId)
                .couponId(couponId)
                .customerId(customerUserId)   // ✅ 고객 User.id 저장
                .build();

        customerCouponRepository.save(cc);
    }

    // ----------------------------------------------------------------------
    // 6) 현재 로그인 customer user 기준, 사용 가능한 쿠폰 개수 (summary용)
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public byte countAvailableCouponsForCurrentCustomerUser(UUID userId) {
        // 1) user → customerId (=userId)
        UUID customerId = resolveCustomerIdForUser(userId);

        var list = customerCouponRepository
                .findAllByCustomerIdAndDeletedAtIsNull(customerId);

        LocalDate today = LocalDate.now(ZONE_ID);

        long count = list.stream()
                .map(cc -> couponRepository.findByIdAndDeletedAtIsNull(cc.getCouponId()).orElse(null))
                .filter(Objects::nonNull)
                .filter(c -> c.getStatus() == CouponStatus.AVAILABLE)
                .filter(c ->
                        (c.getStartDate() == null || !c.getStartDate().isAfter(today)) &&
                                (c.getExpiryDate() == null || !c.getExpiryDate().isBefore(today))
                )
                .count();

        return (byte) count;
    }

    // ----------------------------------------------------------------------
    // HELPER: period → from Instant
    // ----------------------------------------------------------------------
    private Instant resolveFromForPeriod(String period) {
        if (period == null) return null;

        String p = period.trim().toUpperCase();
        LocalDate today = LocalDate.now(ZONE_ID);

        return switch (p) {
            case "TODAY" -> today.atStartOfDay(ZONE_ID).toInstant();
            case "WEEK" -> {
                LocalDate monday = today.with(DayOfWeek.MONDAY);
                yield monday.atStartOfDay(ZONE_ID).toInstant();
            }
            case "MONTH" -> {
                LocalDate firstDay = today.withDayOfMonth(1);
                yield firstDay.atStartOfDay(ZONE_ID).toInstant();
            }
            case "ALL" -> null;
            default -> null;
        };
    }

    // ----------------------------------------------------------------------
    // HELPER: userId → customerId 변환
    //   여기서의 customerId = User.id (다만 CustomerDetail 이 존재하는지 검증만)
    // ----------------------------------------------------------------------
    private UUID resolveCustomerIdForUser(UUID userId) {
        var page1 = PageRequest.of(0, 1);

        var customerOpt = customerDetailRepository
                .findActiveByUserIdOrderByUpdatedDesc(userId, page1)
                .stream()
                .findFirst();

        if (customerOpt.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Customer profili topilmadi"
            );
        }

        // 🔥 중요: CustomerDetail.id 가 아니라 userId 를 반환
        return userId;
    }

    // ----------------------------------------------------------------------
    // MAPPER: CustomerCoupon → CustomerCouponRes
    // ----------------------------------------------------------------------
    private CustomerCouponRes toCustomerCouponRes(CustomerCoupon cc) {
        Coupon coupon = couponRepository.findByIdAndDeletedAtIsNull(cc.getCouponId())
                .orElse(null);

        Instant issuedAt = cc.getCreatedAt();
        Instant usedDateInstant = null;
        if (coupon != null && coupon.getUsedDate() != null) {
            usedDateInstant = coupon.getUsedDate()
                    .atStartOfDay(ZONE_ID)
                    .toInstant();
        }

        return new CustomerCouponRes(
                cc.getId(),
                cc.getStudioId(),
                cc.getCouponId(),
                coupon != null ? coupon.getName() : null,
                coupon != null ? coupon.getDescription() : null,
                coupon != null ? coupon.getDiscountRate() : null,
                coupon != null ? coupon.getDiscountAmount() : null,
                coupon != null ? coupon.getStatus() : null,
                coupon != null ? coupon.getStartDate() : null,
                coupon != null ? coupon.getExpiryDate() : null,
                issuedAt,
                usedDateInstant
        );
    }
}
