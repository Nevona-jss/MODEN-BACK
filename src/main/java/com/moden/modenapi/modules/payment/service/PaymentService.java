package com.moden.modenapi.modules.payment.service;

import com.moden.modenapi.common.enums.PaymentStatus;
import com.moden.modenapi.common.enums.PointType;
import com.moden.modenapi.common.enums.ServiceType;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.coupon.model.Coupon;
import com.moden.modenapi.modules.coupon.repository.CouponRepository;
import com.moden.modenapi.modules.payment.dto.DesignerTipSummaryRes;
import com.moden.modenapi.modules.payment.dto.PaymentCreateReq;
import com.moden.modenapi.modules.payment.dto.PaymentRes;
import com.moden.modenapi.modules.payment.dto.TodaySalesSummaryRes;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
                .paymentStatus(PaymentStatus.UNPAID)
                .serviceTotal(servicePrice)
                .productTotal(BigDecimal.ZERO)
                .couponDiscount(BigDecimal.ZERO)
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
    // 3) 결제 확정 (포인트 + 쿠폰 적용)
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

        BigDecimal servicePrice = studioService.getServicePrice();
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

            // 쿠폰 유효성 체크 (status, 날짜, 고객 혹은 global 여부)
            validateCouponForCustomer(coupon, customerId);

            BigDecimal base = afterPoint; // 포인트 적용 후 금액 기준

            BigDecimal rateDiscount = BigDecimal.ZERO;
            if (coupon.getDiscountRate() != null) {
                rateDiscount = base
                        .multiply(coupon.getDiscountRate())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR); // 원화 기준 0원 단위
            }

            BigDecimal fixedDiscount = defaultZero(coupon.getDiscountAmount());

            couponDiscount = rateDiscount.add(fixedDiscount);

            // 할인 금액이 base 보다 클 수 없게
            if (couponDiscount.compareTo(base) > 0) {
                couponDiscount = base;
            }

            // 쿠폰 상태 변경 (USED)
            coupon.setStatus(com.moden.modenapi.common.enums.CouponStatus.USED);
        }

        // 3-5) 최종 지불 금액
        BigDecimal finalAmount = afterPoint.subtract(couponDiscount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        // 3-6) Payment 엔티티 가져오기 (없으면 생성)
        Payment payment = paymentRepository.findByReservationId(req.reservationId())
                .orElseGet(() -> Payment.builder()
                        .reservationId(reservation.getId())
                        .build()
                );

        payment.setServiceTotal(servicePrice);
        payment.setProductTotal(productTotal);
        payment.setPointsUsed(pointsToUse);
        payment.setCouponDiscount(couponDiscount);
        payment.setTotalAmount(finalAmount);
        payment.setPaymentMethod(req.paymentMethod());
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setCouponId(couponId);

        Payment saved = paymentRepository.save(payment);

        // 3-7) 포인트 USE 기록 남기기
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

    /** 쿠폰이 해당 고객에게 유효한지 검증 (개인 쿠폰 or studio global 쿠폰) */
    private void validateCouponForCustomer(Coupon coupon, UUID customerId) {

        // 상태 체크
        if (coupon.getStatus() != com.moden.modenapi.common.enums.CouponStatus.AVAILABLE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "사용할 수 없는 쿠폰입니다. (status=" + coupon.getStatus() + ")"
            );
        }

        // 날짜 체크
        LocalDate today = LocalDate.now();
        if (coupon.getStartDate() != null && today.isBefore(coupon.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "아직 사용 시작 전인 쿠폰입니다."
            );
        }
        if (coupon.getExpiryDate() != null && today.isAfter(coupon.getExpiryDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이미 만료된 쿠폰입니다."
            );
        }

        // 고객 전용 쿠폰인가 / 스튜디오 global 쿠폰인가
        if (!coupon.isGlobal()) {
            // personal 쿠폰이면 userId 매칭 확인
            if (!coupon.getUserId().equals(customerId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "이 쿠폰은 해당 고객이 사용할 수 없습니다."
                );
            }
        } else {
            // global 이면 userId는 크게 상관없지만, 필요하면 studioId 매칭 등 추가 가능
        }
    }

    private PaymentRes toDto(Payment p) {
        return new PaymentRes(
                p.getId(),
                p.getReservationId(),
                p.getPaymentStatus(),
                p.getPaymentMethod(),
                p.getServiceTotal(),
                p.getProductTotal(),
                p.getCouponDiscount(),
                p.getPointsUsed(),
                p.getTotalAmount(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }


    // ------------------------------
    // 1) Payment list filter – studio / designer / serviceType / status / date range
    // ------------------------------
    @Transactional(readOnly = true)
    public List<PaymentRes> searchPaymentsForList(
            UUID studioId,
            UUID designerId,
            ServiceType serviceType,
            PaymentStatus status,
            LocalDateTime from,
            LocalDateTime to
    ) {
        List<Payment> list = paymentRepository.searchPayments(
                studioId,
                designerId,
                serviceType,
                status,
                from,
                to
        );

        return list.stream()
                .map(this::toDto)
                .toList();
    }

    // ------------------------------
    // 2) Studio uchun: Designer bo‘yicha tip summary
    // ------------------------------
    @Transactional(readOnly = true)
    public List<DesignerTipSummaryRes> studioDesignerTipSummary(
            UUID studioId,
            UUID designerId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        // Tip summary, odatda faqat PAID paymentlardan
        List<Payment> payments = paymentRepository.searchPayments(
                studioId,
                designerId,
                null,                    // serviceType filter yo‘q
                PaymentStatus.PAID,      // faqat to‘langanlar
                from,
                to
        );

        // designerId -> tipSum
        Map<UUID, BigDecimal> tipMap = new HashMap<>();

        for (Payment p : payments) {
            // Reservation orqali designerId va serviceId olamiz
            Reservation r = reservationRepository.findById(p.getReservationId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Payment에 연결된 예약을 찾을 수 없습니다. paymentId=" + p.getId()
                    ));

            StudioService service = studioServiceRepository.findById(r.getServiceId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "해당 서비스 정보를 찾을 수 없습니다. serviceId=" + r.getServiceId()
                    ));

            BigDecimal tipPercent = service.getDesignerTipPercent();
            if (tipPercent == null) {
                tipPercent = BigDecimal.ZERO;
            }

            // Tip = serviceTotal * tipPercent / 100
            BigDecimal tip = p.getServiceTotal()
                    .multiply(tipPercent)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);

            UUID dId = r.getDesignerId();
            tipMap.merge(dId, tip, BigDecimal::add);
        }

        return tipMap.entrySet().stream()
                .map(e -> new DesignerTipSummaryRes(
                        e.getKey(),
                        e.getValue(),
                        e.getValue()      // 지금은 전체 팁 = 서비스 팁
                ))
                .collect(Collectors.toList());
    }


    // ==========================
    // 오늘 총 매출/건수/평균 단가
    // ==========================

    @Transactional(readOnly = true)
    public TodaySalesSummaryRes getTodaySummary(UUID studioId) {

        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();

        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();

        // 🔹 항상 List<Object[]> 로 받는다
        List<Object[]> rows = paymentRepository.aggregateSalesForPeriod(
                studioId,
                PaymentStatus.PAID,
                start,
                end
        );

        Object[] row;
        if (rows == null || rows.isEmpty()) {
            // 결과가 아예 없을 때
            row = new Object[]{null, 0L, null};
        } else {
            row = rows.get(0);
        }

        // row[0] = sum(totalAmount)
        // row[1] = count(*)
        // row[2] = avg(totalAmount)

        BigDecimal totalSales = toBigDecimal(row[0]);
        long paymentCount = (row[1] == null) ? 0L : ((Number) row[1]).longValue();

        BigDecimal averageAmount = BigDecimal.ZERO;
        if (paymentCount > 0) {
            averageAmount = toBigDecimal(row[2]);
            // 통화 기준이면 소수점 0자리로 맞추거나 필요에 따라 scale 조정
            averageAmount = averageAmount.setScale(0, RoundingMode.HALF_UP);
        }

        return new TodaySalesSummaryRes(
                today,
                totalSales,
                paymentCount,
                averageAmount
        );
    }

    /**
     * 다양한 형태의 숫자(Object, Number, Object[]) 를 BigDecimal 로 안전하게 변환
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        // 🔹 만약 또 Object[] 한 번 더 감싸져 있으면 첫 번째 요소를 다시 처리
        if (value instanceof Object[] arr) {
            if (arr.length == 0) {
                return BigDecimal.ZERO;
            }
            return toBigDecimal(arr[0]); // 재귀 한 번 더
        }

        if (value instanceof BigDecimal bd) {
            return bd;
        }

        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }

        // 여기에 걸리면 진짜 이상한 타입이 들어온 것
        throw new IllegalArgumentException("Unexpected numeric type: " + value.getClass());
    }


    @Transactional(readOnly = true)
    public PaymentStatus getPaymentStatusByReservationId(UUID reservationId) {
        return paymentRepository.findByReservationId(reservationId)
                .map(Payment::getPaymentStatus)
                // payment 가 아직 없으면 UNPAID 로 보고 싶으면 기본값을 UNPAID 로
                .orElse(PaymentStatus.UNPAID);
    }
}
