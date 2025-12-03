package com.moden.modenapi.modules.payment.service;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.common.enums.PaymentStatus;
import com.moden.modenapi.common.enums.PointType;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.coupon.model.Coupon;
import com.moden.modenapi.modules.coupon.model.CustomerCoupon;
import com.moden.modenapi.modules.coupon.repository.CouponRepository;
import com.moden.modenapi.modules.coupon.repository.CustomerCouponRepository;
import com.moden.modenapi.modules.payment.dto.*;
import com.moden.modenapi.modules.payment.model.Payment;
import com.moden.modenapi.modules.payment.repository.PaymentRepository;
import com.moden.modenapi.modules.point.model.Point;
import com.moden.modenapi.modules.point.repository.PointRepository;
import com.moden.modenapi.modules.reservation.model.Reservation;
import com.moden.modenapi.modules.reservation.repository.ReservationRepository;
import com.moden.modenapi.modules.studioservice.model.StudioService;
import com.moden.modenapi.modules.studioservice.repository.StudioServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService extends BaseService<Payment> {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final StudioServiceRepository studioServiceRepository;
    private final PointRepository pointRepository;
    private final CouponRepository couponRepository;
    private final CustomerCouponRepository customerCouponRepository;

    @Override
    protected JpaRepository<Payment, UUID> getRepository() {
        return paymentRepository;
    }

    // ------------------------------ //
    // 1) 예약 생성 시 UNPAID Payment 생성
    // ------------------------------ //
    public void createUnpaidPaymentForReservation(Reservation reservation) {

        StudioService studioService = studioServiceRepository.findById(reservation.getServiceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 서비스 정보를 찾을 수 없습니다. serviceId=" + reservation.getServiceId()
                ));

        BigDecimal servicePrice = studioService.getServicePrice();

        Payment payment = Payment.builder()
                .reservationId(reservation.getId())
                .paymentStatus(PaymentStatus.PENDING)
                .serviceTotal(servicePrice)
                .productTotal(BigDecimal.ZERO)
                .pointsUsed(BigDecimal.ZERO)
                .totalAmount(servicePrice) // 처음엔 서비스 금액 그대로
                .build();

        paymentRepository.save(payment);
    }

    // ------------------------------ //
    // 2) 예약 기준 결제 조회 (payment detail)
    // ------------------------------ //
    @Transactional(readOnly = true)
    public PaymentRes getByReservation(UUID reservationId) {
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 예약에 대한 결제 정보가 없습니다."
                ));
        return toDto(payment);
    }

    // ------------------------------ //
    // 3) 결제 확정 (포인트 + 쿠폰 + Tip 계산)
    // ------------------------------ //
    public PaymentRes confirmPayment(PaymentCreateReq req) {

        // 3-1) 예약 조회
        Reservation reservation = reservationRepository.findById(req.reservationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 예약을 찾을 수 없습니다. reservationId=" + req.reservationId()
                ));

        // customerId (포인트/쿠폰 검증 용도)
        UUID customerId = reservation.getCustomerId();

        // 3-2) 서비스 금액 조회
        StudioService studioService = studioServiceRepository.findById(reservation.getServiceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 서비스 정보를 찾을 수 없습니다. serviceId=" + reservation.getServiceId()
                ));

        BigDecimal servicePrice = defaultZero(studioService.getServicePrice());
        BigDecimal productTotal = defaultZero(req.productTotal());

        // 서비스 + 제품 = 기본 합계
        BigDecimal subTotal = servicePrice.add(productTotal);

        // 3-3) 현재 활성 포인트 계산
        BigDecimal activePoint = calcActivePoint(customerId);
        BigDecimal pointsToUse = defaultZero(req.pointsToUse());

        if (pointsToUse.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "사용 포인트는 음수가 될 수 없습니다."
            );
        }

        if (pointsToUse.compareTo(activePoint) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "보유 포인트가 부족합니다. (보유: " + activePoint + ", 요청: " + pointsToUse + ")"
            );
        }

        // 포인트 차감 후 금액
        BigDecimal afterPoint = subTotal.subtract(pointsToUse);
        if (afterPoint.compareTo(BigDecimal.ZERO) < 0) {
            afterPoint = BigDecimal.ZERO;
        }

        // 3-4) 쿠폰 할인 계산
        BigDecimal couponDiscount = BigDecimal.ZERO;
        UUID couponId = req.couponId();

        if (couponId != null) {
            Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "해당 쿠폰을 찾을 수 없습니다. couponId=" + couponId
                    ));

            // 쿠폰 유효성 체크 (상태 + 날짜)
            validateCouponForCustomer(coupon, customerId);

            BigDecimal base = afterPoint; // 포인트 적용 후 금액 기준
            couponDiscount = computeCouponDiscount(base, coupon);

            // 쿠폰 상태 변경 (USED)
            coupon.setStatus(CouponStatus.USED);
            coupon.setUsedDate(LocalDate.now(ZoneId.of("Asia/Tashkent")));
            couponRepository.save(coupon);
        }

        // 3-5) 최종 지불 금액
        BigDecimal finalAmount = afterPoint.subtract(couponDiscount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        // 3-6) 디자이너 Tip 계산 (service + product 기준)
        BigDecimal tipPercent = defaultZero(studioService.getDesignerTipPercent());
        // Tip = (서비스 + 제품) * tipPercent / 100
        BigDecimal tipBase = servicePrice.add(productTotal);
        if (tipBase.compareTo(BigDecimal.ZERO) < 0) {
            tipBase = BigDecimal.ZERO;
        }

        BigDecimal designerTip = tipBase
                .multiply(tipPercent)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);

        // 3-7) Payment 엔티티 가져오기 (없으면 생성 → UPDATE 형태로 사용)
        Payment payment = paymentRepository.findByReservationId(req.reservationId())
                .orElseGet(() -> Payment.builder()
                        .reservationId(reservation.getId())
                        .build()
                );

        payment.setServiceTotal(servicePrice);
        payment.setProductTotal(productTotal);
        payment.setPointsUsed(pointsToUse);
        payment.setTotalAmount(finalAmount);
        payment.setPaymentMethod(req.paymentMethod());
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setCouponId(couponId);
        payment.setDesignerTipAmount(designerTip);   // ✅ Tip 저장

        Payment saved = paymentRepository.save(payment);

        // 3-8) 포인트 USE 기록 남기기
        if (pointsToUse.compareTo(BigDecimal.ZERO) > 0) {
            Point usePoint = Point.builder()
                    .userId(customerId)
                    .paymentId(saved.getId())
                    .type(PointType.USED)
                    .amount(pointsToUse)
                    .title("헤어샵 결제 포인트 사용")
                    .build();
            pointRepository.save(usePoint);
        }

        return toDto(saved);
    }

    // ------------------------------ //
    // 내부 유틸 메서드
    // ------------------------------ //

    private BigDecimal defaultZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 현재 유저의 활성 포인트 (EARN - USE) */
    private BigDecimal calcActivePoint(UUID userId) {
        List<Point> list = pointRepository.findByUserId(userId);

        return list.stream()
                .map(p -> {
                    if (p.getType() == PointType.EARNED) {
                        return p.getAmount();
                    } else if (p.getType() == PointType.USED) {
                        return p.getAmount().negate();
                    } else {
                        return BigDecimal.ZERO;
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 쿠폰이 "사용 가능한 상태인지" 간단히 검증
     *  - 상태: AVAILABLE
     *  - 날짜: startDate ~ expiryDate 범위
     *  - customerId 는 현재는 비즈니스 제약에 사용하지 않고, 향후 확장 여지로 둠
     */
    private void validateCouponForCustomer(Coupon coupon, UUID customerId) {

        if (coupon.getStatus() != CouponStatus.AVAILABLE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "현재 상태에서 사용할 수 없는 쿠폰입니다."
            );
        }

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Tashkent"));

        if (coupon.getStartDate() != null && coupon.getStartDate().isAfter(today)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "아직 사용 시작 전인 쿠폰입니다."
            );
        }
        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(today)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이미 만료된 쿠폰입니다."
            );
        }

        // TODO: 필요하면 customerId 기반 추가 제약 (특정 고객만 사용 가능 등)을 여기서 확장
    }

    // cc: CustomerCoupon, coupon: Coupon, customerId: 현재 로그인 고객 ID
    private void validateCustomerCanUseCoupon(CustomerCoupon cc, Coupon coupon, UUID customerId) {
        // 1) 이 쿠폰 소유 고객인지 확인
        if (!cc.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "이 쿠폰은 해당 고객의 쿠폰이 아닙니다."
            );
        }

        // 2) studio 일치 확인 (쿠폰 정책과 발급된 쿠폰이 같은 헤어샵인지)
        if (!coupon.getStudioId().equals(cc.getStudioId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "쿠폰 정보와 발급 정보의 스튜디오가 일치하지 않습니다."
            );
        }

        // 3) 상태 확인
        if (coupon.getStatus() != CouponStatus.AVAILABLE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "현재 상태에서 사용할 수 없는 쿠폰입니다."
            );
        }

        // 4) 날짜 유효성 (오늘 기준 사용 가능 기간인지)
        var today = LocalDate.now(ZoneId.of("Asia/Tashkent"));

        if (coupon.getStartDate() != null && coupon.getStartDate().isAfter(today)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "아직 사용 시작 전인 쿠폰입니다."
            );
        }
        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(today)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이미 만료된 쿠폰입니다."
            );
        }
    }

    public void useCoupon(UUID customerCouponId, UUID currentCustomerId) {
        CustomerCoupon cc = customerCouponRepository.findByIdAndDeletedAtIsNull(customerCouponId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));

        Coupon coupon = couponRepository.findByIdAndDeletedAtIsNull(cc.getCouponId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "쿠폰 정책을 찾을 수 없습니다."));

        validateCustomerCanUseCoupon(cc, coupon, currentCustomerId);

        // 이후 실제 사용 처리 (상태 변경, usedDate 세팅 등)
        coupon.setStatus(CouponStatus.USED);
        coupon.setUsedDate(LocalDate.now(ZoneId.of("Asia/Tashkent")));
        couponRepository.save(coupon);
    }

    /**
     * 쿠폰 할인 계산 (정율 + 정액 모두 적용)
     * base: 포인트 적용 후 금액
     */
    private BigDecimal computeCouponDiscount(BigDecimal base, Coupon coupon) {
        if (coupon == null || base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal rateDiscount = BigDecimal.ZERO;
        BigDecimal amountDiscount = BigDecimal.ZERO;

        if (coupon.getDiscountRate() != null) {
            rateDiscount = base
                    .multiply(coupon.getDiscountRate())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
        }

        if (coupon.getDiscountAmount() != null) {
            amountDiscount = coupon.getDiscountAmount();
        }

        BigDecimal totalDiscount = rateDiscount.add(amountDiscount);

        if (totalDiscount.compareTo(base) > 0) {
            totalDiscount = base;
        }
        if (totalDiscount.compareTo(BigDecimal.ZERO) < 0) {
            totalDiscount = BigDecimal.ZERO;
        }

        return totalDiscount;
    }

    // 🔹 Payment → PaymentRes 변환 시 couponDiscount 는 couponId 기반으로 다시 계산
    private PaymentRes toDto(Payment p) {

        BigDecimal couponDiscount = calcCouponDiscountFromPayment(p);

        return new PaymentRes(
                p.getId(),
                p.getReservationId(),
                p.getPaymentStatus(),
                p.getPaymentMethod(),
                p.getServiceTotal(),
                p.getProductTotal(),
                couponDiscount,
                p.getPointsUsed(),
                p.getTotalAmount(),
                p.getDesignerTipAmount(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    /** couponId, rate, amount 기반으로 할인 금액 재계산 */
    private BigDecimal calcCouponDiscountFromPayment(Payment p) {
        UUID couponId = p.getCouponId();
        if (couponId == null) {
            return BigDecimal.ZERO;
        }

        Coupon coupon = couponRepository.findById(couponId).orElse(null);
        if (coupon == null) {
            return BigDecimal.ZERO;
        }

        // "서비스 + 제품 - 포인트" 기준으로 다시 계산
        BigDecimal base = defaultZero(p.getServiceTotal())
                .add(defaultZero(p.getProductTotal()))
                .subtract(defaultZero(p.getPointsUsed()));

        if (base.compareTo(BigDecimal.ZERO) < 0) {
            base = BigDecimal.ZERO;
        }

        return computeCouponDiscount(base, coupon);
    }

    // ------------------------------
    // Payment list / summary 부분 (기존 코드 정리)
    // ------------------------------
    @Transactional(readOnly = true)
    public List<PaymentRes> searchPaymentsForList(
            UUID studioId,
            UUID designerId,
            String serviceName,
            PaymentStatus status,
            LocalDateTime from,
            LocalDateTime to
    ) {
        LocalDate fromDate = (from != null) ? from.toLocalDate() : null;
        LocalDate toDate   = (to != null)   ? to.toLocalDate()   : null;

        List<Payment> list = paymentRepository.searchPayments(
                studioId,
                designerId,
                serviceName,
                status,
                fromDate,
                toDate
        );

        return list.stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DesignerTipSummaryRes> studioDesignerTipSummary(
            UUID studioId,
            UUID designerId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        LocalDate fromDate = (from != null) ? from.toLocalDate() : null;
        LocalDate toDate   = (to != null)   ? to.toLocalDate()   : null;

        List<Payment> payments = paymentRepository.searchPayments(
                studioId,
                designerId,
                null,
                PaymentStatus.PAID,
                fromDate,
                toDate
        );

        Map<UUID, BigDecimal> tipMap = new HashMap<>();

        for (Payment p : payments) {
            Reservation r = reservationRepository.findById(p.getReservationId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Payment에 연결된 예약을 찾을 수 없습니다. paymentId=" + p.getId()
                    ));

            UUID dId = r.getDesignerId();
            BigDecimal tip = defaultZero(p.getDesignerTipAmount());
            tipMap.merge(dId, tip, BigDecimal::add);
        }

        return tipMap.entrySet().stream()
                .map(e -> new DesignerTipSummaryRes(
                        e.getKey(),
                        e.getValue(),
                        e.getValue()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TodaySalesSummaryRes getTodaySummary(UUID studioId) {

        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();

        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();

        List<Object[]> rows = paymentRepository.aggregateSalesForPeriod(
                studioId,
                PaymentStatus.PAID,
                start,
                end
        );

        Object[] row;
        if (rows == null || rows.isEmpty()) {
            row = new Object[]{null, 0L, null};
        } else {
            row = rows.get(0);
        }

        BigDecimal totalSales = toBigDecimal(row[0]);
        long paymentCount = (row[1] == null) ? 0L : ((Number) row[1]).longValue();

        BigDecimal averageAmount = BigDecimal.ZERO;
        if (paymentCount > 0) {
            averageAmount = toBigDecimal(row[2]);
            averageAmount = averageAmount.setScale(0, RoundingMode.HALF_UP);
        }

        return new TodaySalesSummaryRes(
                today,
                totalSales,
                paymentCount,
                averageAmount
        );
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof Object[] arr) {
            if (arr.length == 0) {
                return BigDecimal.ZERO;
            }
            return toBigDecimal(arr[0]);
        }

        if (value instanceof BigDecimal bd) {
            return bd;
        }

        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }

        throw new IllegalArgumentException("Unexpected numeric type: " + value.getClass());
    }

    @Transactional(readOnly = true)
    public PaymentStatus getPaymentStatusByReservationId(UUID reservationId) {
        return paymentRepository.findByReservationId(reservationId)
                .map(Payment::getPaymentStatus)
                .orElse(PaymentStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<PaymentListItemRes> getStudioPaymentList(
            UUID studioId,
            UUID designerId,
            String serviceName,
            LocalDateTime from,
            LocalDateTime to,
            PaymentStatus status,
            int page,
            int size
    ) {
        LocalDate fromDate = (from != null) ? from.toLocalDate() : null;
        LocalDate toDate   = (to != null)   ? to.toLocalDate()   : null;

        List<Payment> payments = paymentRepository.searchPayments(
                studioId,
                designerId,
                serviceName,
                status,
                fromDate,
                toDate
        );

        List<PaymentListItemRes> all = payments.stream()
                .map(p -> {
                    Reservation r = reservationRepository.findById(p.getReservationId())
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Payment에 연결된 예약을 찾을 수 없습니다. paymentId=" + p.getId()
                            ));

                    StudioService s = studioServiceRepository.findById(r.getServiceId())
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "해당 서비스 정보를 찾을 수 없습니다. serviceId=" + r.getServiceId()
                            ));

                    return toListItemDto(p, r, s);
                })
                .toList();

        return paginate(all, page, size);
    }

    @Transactional(readOnly = true)
    public List<PaymentListItemRes> getDesignerPaymentList(
            UUID designerId,
            String serviceName,
            LocalDateTime from,
            LocalDateTime to,
            PaymentStatus status,
            int page,
            int size
    ) {
        LocalDate fromDate = (from != null) ? from.toLocalDate() : null;
        LocalDate toDate   = (to != null)   ? to.toLocalDate()   : null;

        List<Payment> payments = paymentRepository.searchPayments(
                null,
                designerId,
                serviceName,
                status,
                fromDate,
                toDate
        );

        List<PaymentListItemRes> all = payments.stream()
                .map(p -> {
                    Reservation r = reservationRepository.findById(p.getReservationId())
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Payment에 연결된 예약을 찾을 수 없습니다. paymentId=" + p.getId()
                            ));

                    StudioService s = studioServiceRepository.findById(r.getServiceId())
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "해당 서비스 정보를 찾을 수 없습니다. serviceId=" + r.getServiceId()
                            ));

                    return toListItemDto(p, r, s);
                })
                .toList();

        return paginate(all, page, size);
    }

    private List<PaymentListItemRes> paginate(List<PaymentListItemRes> list, int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (size > 10) size = 10;

        int fromIndex = page * size;
        if (fromIndex >= list.size()) {
            return List.of();
        }

        int toIndex = Math.min(fromIndex + size, list.size());
        return list.subList(fromIndex, toIndex);
    }

    private PaymentListItemRes toListItemDto(Payment p, Reservation r, StudioService s) {

        String customerFullName = null;
        String designerFullName = null;

        Instant consultCompletedAt = null;
        // 필요하면 reservation의 일시 필드 매핑

        return new PaymentListItemRes(
                p.getId(),
                p.getReservationId(),
                consultCompletedAt,
                customerFullName,
                designerFullName,
                s.getServiceName(),
                p.getTotalAmount(),
                p.getPaymentStatus(),
                p.getDesignerTipAmount()
        );
    }
}
