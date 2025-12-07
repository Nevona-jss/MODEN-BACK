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
import com.moden.modenapi.modules.product.model.StudioProduct;
import com.moden.modenapi.modules.product.repository.StudioProductRepository;
import com.moden.modenapi.modules.reservation.model.Reservation;
import com.moden.modenapi.modules.reservation.repository.ReservationRepository;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import com.moden.modenapi.modules.studioservice.model.StudioService;
import com.moden.modenapi.modules.studioservice.repository.StudioServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

import static com.moden.modenapi.common.utils.CurrentUserUtil.currentUserId;
import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;

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
    private final StudioProductRepository studioProductRepository;
    private final HairStudioDetailRepository studioDetailRepository;



    @Override
    protected JpaRepository<Payment, UUID> getRepository() {
        return paymentRepository;
    }

    // ------------------------------ //
    // 1) 예약 생성 시 UNPAID Payment 생성
    // ------------------------------ //

    /**
     * Reservation 생성 직후, UNPAID(PENDING) Payment 생성.
     * totalAmount 파라미터는 현재는 참고용(double),
     * 실제 서비스 가격은 reservation.serviceIds 기반으로 다시 계산.
     */
    public void createUnpaidPaymentForReservation(Reservation reservation, double totalAmount) {

        List<UUID> serviceIds = reservation.getServiceIds();
        if (serviceIds == null || serviceIds.isEmpty()) {
            // 서비스가 전혀 없는 예약이라면 넘어온 totalAmount 로만 생성
            BigDecimal serviceTotal = BigDecimal.valueOf(totalAmount);

            Payment payment = Payment.builder()
                    .reservationId(reservation.getId())
                    .paymentStatus(PaymentStatus.PENDING)
                    .serviceTotal(serviceTotal)
                    .productTotal(BigDecimal.ZERO)
                    .pointsUsed(BigDecimal.ZERO)
                    .totalAmount(serviceTotal)
                    .build();

            paymentRepository.save(payment);
            return;
        }

        // 여러 서비스 가격 합산
        List<StudioService> services = studioServiceRepository.findAllById(serviceIds);
        BigDecimal serviceTotal = services.stream()
                .map(StudioService::getServicePrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Payment payment = Payment.builder()
                .reservationId(reservation.getId())
                .paymentStatus(PaymentStatus.PENDING)
                .serviceTotal(serviceTotal)
                .productTotal(BigDecimal.ZERO)
                .pointsUsed(BigDecimal.ZERO)
                .totalAmount(serviceTotal) // 초기 totalAmount = 서비스 합계
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

// PaymentService 내부

    // 추가 필드 필요

    public PaymentRes confirmPayment(UUID paymentId, PaymentCreateReq req) {

        // 1) Payment (UNPAID) 조회
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 결제 정보를 찾을 수 없습니다. paymentId=" + paymentId
                ));

        // 2) 예약 조회
        UUID reservationId = payment.getReservationId();
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 예약을 찾을 수 없습니다. reservationId=" + reservationId
                ));

        UUID customerId = reservation.getCustomerId();

        // 3) 서비스 금액 조회 (여러 서비스)
        List<UUID> serviceIds = reservation.getServiceIds();
        if (serviceIds == null || serviceIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "예약에 연결된 서비스가 없습니다."
            );
        }

        List<StudioService> services = studioServiceRepository.findAllById(serviceIds);
        if (services.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "예약에 연결된 서비스를 찾을 수 없습니다."
            );
        }

        BigDecimal servicePrice = services.stream()
                .map(StudioService::getServicePrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3-B) 상품 목록 기반 productTotal / productTip 계산
        List<PaymentProductLineReq> productLines =
                Optional.ofNullable(req.products()).orElse(List.of());

        BigDecimal productTotal = BigDecimal.ZERO;
        BigDecimal productTip   = BigDecimal.ZERO;
        List<UUID> productIdsForPayment = new ArrayList<>();

        if (!productLines.isEmpty()) {
            List<UUID> productIds = productLines.stream()
                    .map(PaymentProductLineReq::productId)
                    .toList();

            List<StudioProduct> products = studioProductRepository.findAllById(productIds);
            Map<UUID, StudioProduct> productMap = products.stream()
                    .collect(Collectors.toMap(StudioProduct::getId, p -> p));

            for (PaymentProductLineReq line : productLines) {
                StudioProduct product = productMap.get(line.productId());
                if (product == null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "존재하지 않는 상품이 있습니다. productId=" + line.productId()
                    );
                }

                int qtyInt = line.quantity() == null ? 0 : line.quantity();
                if (qtyInt <= 0) continue;

                productIdsForPayment.add(line.productId());

                BigDecimal qty   = BigDecimal.valueOf(qtyInt);
                BigDecimal price = defaultZero(product.getPrice());
                BigDecimal lineTotal = price.multiply(qty);

                productTotal = productTotal.add(lineTotal);

                BigDecimal tipPercent = defaultZero(product.getDesignerTipPercent());
                if (tipPercent.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal lineTip = lineTotal
                            .multiply(tipPercent)
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
                    productTip = productTip.add(lineTip);
                }
            }
        }

        // 서비스 + 제품 = 기본 합계
        BigDecimal subTotal = servicePrice.add(productTotal);

        // 4) 현재 활성 포인트 계산
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

        // 5) 쿠폰 할인 계산 (service + product 기준)
        BigDecimal couponDiscount = BigDecimal.ZERO;
        UUID couponId = req.couponId();

        if (couponId != null) {
            Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "해당 쿠폰을 찾을 수 없습니다. couponId=" + couponId
                    ));

            validateCouponForCustomer(coupon, customerId);

            // 기준: 서비스 + 제품 (포인트 적용 전)
            BigDecimal base = subTotal;
            couponDiscount = computeCouponDiscount(base, coupon);

            coupon.setStatus(CouponStatus.USED);
            coupon.setUsedDate(LocalDate.now(ZoneId.of("Asia/Tashkent")));
            couponRepository.save(coupon);
        }

        // 6) 최종 지불 금액
        BigDecimal finalAmount = afterPoint.subtract(couponDiscount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        // 7-A) 서비스 Tip
        BigDecimal serviceTip = services.stream()
                .map(s -> {
                    BigDecimal price   = defaultZero(s.getServicePrice());
                    BigDecimal percent = defaultZero(s.getDesignerTipPercent());

                    if (price.compareTo(BigDecimal.ZERO) <= 0 ||
                            percent.compareTo(BigDecimal.ZERO) <= 0) {
                        return BigDecimal.ZERO;
                    }

                    return price
                            .multiply(percent)
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 7-B) 최종 디자이너 Tip = 서비스 Tip + 상품 Tip
        BigDecimal designerTip = serviceTip.add(productTip);

        // 8) Payment 갱신
        payment.setServiceTotal(servicePrice);
        payment.setProductTotal(productTotal);
        payment.setPointsUsed(pointsToUse);
        payment.setTotalAmount(finalAmount);
        payment.setPaymentMethod(req.paymentMethod());
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setCouponId(couponId);
        payment.setDesignerTipAmount(designerTip);
        payment.setProductIds(productIdsForPayment);

        Payment saved = paymentRepository.save(payment);

        // 9) 포인트 USE 기록
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
    private BigDecimal calcCouponDiscountFromPayment(Payment p) {
        UUID couponId = p.getCouponId();
        if (couponId == null) {
            return BigDecimal.ZERO;
        }

        Coupon coupon = couponRepository.findById(couponId).orElse(null);
        if (coupon == null) {
            return BigDecimal.ZERO;
        }

        // ✅ 기준: 서비스 + 제품 (포인트는 빼지 않음)
        BigDecimal base = defaultZero(p.getServiceTotal())
                .add(defaultZero(p.getProductTotal()));

        if (base.compareTo(BigDecimal.ZERO) < 0) {
            base = BigDecimal.ZERO;
        }

        return computeCouponDiscount(base, coupon);
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
    }

    private void validateCustomerCanUseCoupon(CustomerCoupon cc, Coupon coupon, UUID customerId) {
        // 1) 이 쿠폰 소유 고객인지 확인
        if (!cc.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "이 쿠폰은 해당 고객의 쿠폰이 아닙니다."
            );
        }

        // 2) studio 일치 확인
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

        // 4) 날짜 유효성
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

    // ------------------------------
    // Payment list / summary 부분
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
                status,
                fromDate,
                toDate
        );

        // 단순 DTO 변환 (serviceName 필터는 여기서도 in-memory로 가능하지만
        // 지금은 getStudioPaymentList / getDesignerPaymentList 에서 처리)
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
    public TodaySalesSummaryRes getTodaySummary(UUID userId) {

        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();

        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end   = today.plusDays(1).atStartOfDay(zone).toInstant();

        List<Object[]> rows = paymentRepository.aggregateSalesForPeriod(
                userId,
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
    public PaymentListPageRes getStudioPaymentList(
            UUID studioId,
            UUID designerId,
            String serviceName,
            LocalDateTime from,
            LocalDateTime to,
            PaymentStatus status,
            int page,   // 0-based로 들어온다고 가정 (page=0이면 첫 페이지)
            int size
    ) {
        // ---- 1) page / size 보정 ----
        int safeSize   = (size <= 0) ? 10 : size;
        int pageIndex  = (page < 0) ? 0 : page;   // 0-based index
        int pageNumber = pageIndex + 1;           // 응답에는 1-based 로 내려줌

        // ---- 2) LocalDate 로 변환 ----
        LocalDate fromDate = (from != null) ? from.toLocalDate() : null;
        LocalDate toDate   = (to   != null) ? to.toLocalDate()   : null;

        // ---- 3) DB 검색 ----
        List<Payment> payments = paymentRepository.searchPayments(
                studioId,
                designerId,
                status,
                fromDate,
                toDate
        );

        // ---- 4) Payment + Reservation → DTO 매핑 ----
        List<PaymentListItemRes> all = payments.stream()
                .map(p -> {
                    Reservation r = reservationRepository.findById(p.getReservationId())
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Payment에 연결된 예약을 찾을 수 없습니다. paymentId=" + p.getId()
                            ));

                    String serviceNames = buildServiceNames(r);
                    return toListItemDto(p, r, serviceNames);
                })
                .toList();

        // ---- 5) serviceName in-memory filter ----
        if (serviceName != null && !serviceName.isBlank()) {
            String keyword = serviceName.trim();
            all = all.stream()
                    .filter(item -> item.serviceName() != null
                            && item.serviceName().contains(keyword))
                    .toList();
        }

        // ---- 6) 전체 개수 ----
        long totalCount = all.size();

        // ---- 7) 인메모리 pagination ----
        List<PaymentListItemRes> pageItems = paginate(all, pageIndex, safeSize);

        // ---- 8) Page DTO 로 감싸서 리턴 ----
        return new PaymentListPageRes(
                totalCount,
                safeSize,
                pageNumber,
                pageItems
        );
    }

    /**
     * 0-based pageIndex, size 개수만큼 잘라주는 인메모리 pagination 헬퍼
     */
    private <T> List<T> paginate(List<T> list, int pageIndex, int size) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        if (size <= 0) size = 10;
        if (pageIndex < 0) pageIndex = 0;

        int fromIndex = pageIndex * size;
        if (fromIndex >= list.size()) {
            return List.of();
        }

        int toIndex = Math.min(fromIndex + size, list.size());
        return list.subList(fromIndex, toIndex);
    }


    @Transactional(readOnly = true)
    public TodaySalesSummaryRes getTodaySummaryForCurrentUser(UUID userId) {


        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();

        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end   = today.plusDays(1).atStartOfDay(zone).toInstant();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isStudioOwner = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HAIR_STUDIO"));
        boolean isDesigner = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DESIGNER"));

        UUID keyId;

        if (isStudioOwner) {
            // 1) userId = studio owner userId deb qabul qilamiz
            // ownerUserId bo‘yicha studio entitini topamiz
            HairStudioDetail studio = studioDetailRepository
                    .findByOwnerUserId(userId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Studio not found for current owner"
                    ));

            // ⚠ MUHIM:
            // Reservation.studioId ga aynan nima saqlaganingga qarab tanlaysan:
            // Agar studioId = HairStudioDetail.id bo‘lsa:
            //   keyId = studio.getId();
            // Agar studioId = ownerUserId bo‘lsa:
            //   keyId = userId;

            keyId = studio.getUserId();   // yoki keyId = userId; (schema'ingga moslab tanla)

        } else if (isDesigner) {
            // Designer bo‘lsa, Reservation.designerId = designer userId deb qabul qilamiz
            keyId = userId;
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "오늘 매출 요약은 스튜디오 또는 디자이너만 조회할 수 있습니다."
            );
        }


        List<Object[]> rows = paymentRepository.aggregateSalesForPeriod(
                keyId,
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
            averageAmount = toBigDecimal(row[2]).setScale(0, RoundingMode.HALF_UP);
        }

        return new TodaySalesSummaryRes(
                today,
                totalSales,
                paymentCount,
                averageAmount
        );
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

                    String serviceNames = buildServiceNames(r);

                    return toListItemDto(p, r, serviceNames);
                })
                .toList();

        if (serviceName != null && !serviceName.isBlank()) {
            String keyword = serviceName.trim();
            all = all.stream()
                    .filter(item -> item.serviceName() != null
                            && item.serviceName().contains(keyword))
                    .toList();
        }

        return paginate(all, page, size);
    }

    /**
     * 예약에 연결된 serviceIds 기준으로 서비스 이름들을 ", " 로 join
     */
    private String buildServiceNames(Reservation r) {
        List<UUID> serviceIds = r.getServiceIds();
        if (serviceIds == null || serviceIds.isEmpty()) {
            return "";
        }

        List<StudioService> services = studioServiceRepository.findAllById(serviceIds);

        return services.stream()
                .map(StudioService::getServiceName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }


    private PaymentListItemRes toListItemDto(Payment p, Reservation r, String serviceNames) {

        String customerFullName = null;
        String designerFullName = null;

        Instant consultCompletedAt = null;

        return new PaymentListItemRes(
                p.getId(),
                p.getReservationId(),
                consultCompletedAt,
                customerFullName,
                designerFullName,
                serviceNames,
                p.getTotalAmount(),
                p.getPaymentStatus(),
                p.getDesignerTipAmount()
        );
    }
}
