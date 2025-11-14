package com.moden.modenapi.modules.point.service;

import com.moden.modenapi.common.enums.PointType;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.point.dto.*;
import com.moden.modenapi.modules.point.model.Point;
import com.moden.modenapi.modules.point.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PointService extends BaseService<Point> {

    private final PointRepository pointRepository;
    private final StudioPointPolicyService studioPointPolicyService;

    @Override
    protected PointRepository getRepository() {
        return pointRepository;
    }

    /* ================== 기간 처리 (TODAY/WEEK/MONTH) ================== */

    private Instant resolveFromForPeriod(String periodKey) {
        if (periodKey == null) return null;

        String key = periodKey.trim().toUpperCase();
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zone);

        return switch (key) {
            case "TODAY" -> today.atStartOfDay(zone).toInstant();
            case "WEEK"  -> today.minusDays(6).atStartOfDay(zone).toInstant();   // 최근 7일
            case "MONTH" -> today.withDayOfMonth(1).atStartOfDay(zone).toInstant(); // 이번 달 1일
            default -> null;  // ALL
        };
    }

    /* ================== Customer용 리스트 ================== */

    @Transactional(readOnly = true)
    public List<PointCustomerRes> listForCustomer(UUID userId, PointType type, String period) {
        List<Point> base;
        if (type != null) {
            base = pointRepository.findAllByUserIdAndTypeAndDeletedAtIsNull(userId, type);
        } else {
            base = pointRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        }

        Instant from = resolveFromForPeriod(period);

        return base.stream()
                .filter(p -> {
                    if (from == null) return true;
                    Instant c = p.getCreatedAt();
                    return !c.isBefore(from);
                })
                .map(this::mapToCustomerRes)
                .toList();
    }

    private String resolveServiceName(UUID paymentId) {
        if (paymentId == null) {
            // Studio manual point면 title 이 Studio Point
            return null;
        }
        // TODO: paymentRepository + serviceRepository 로 실제 서비스명 resolve
        return null;
    }

    private PointCustomerRes mapToCustomerRes(Point p) {
        String serviceName = resolveServiceName(p.getPaymentId());
        return new PointCustomerRes(
                p.getId(),
                p.getTitle(),
                p.getType(),
                p.getAmount(),
                p.getCreatedAt(),
                serviceName
        );
    }

    /* ================== 공용 MAPPER ================== */

    private PointRes mapToRes(Point p) {
        return new PointRes(
                p.getId(),
                p.getUserId(),
                p.getPaymentId(),
                p.getTitle(),
                p.getType(),
                p.getAmount(),
                p.getCreatedAt()
        );
    }

    /* ✅ 단일 포인트 조회 (Studio/Admin) */
    @Transactional(readOnly = true)
    public PointRes getPoint(UUID pointId) {
        Point p = pointRepository.findByIdAndDeletedAtIsNull(pointId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Point not found"));
        return mapToRes(p);
    }

    /* ================== PAYMENT → POINT (INTERNAL) ================== */

    public PointRes earnFromPayment(
            UUID studioId,
            UUID userId,
            UUID paymentId,
            BigDecimal paymentAmount
    ) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        BigDecimal ratePercent = studioPointPolicyService.resolveRateForStudio(studioId); // ex: 5.00

        BigDecimal raw = paymentAmount
                .multiply(ratePercent)
                .divide(BigDecimal.valueOf(100));  // 5.00% → 0.05

        BigDecimal pointAmount = raw.setScale(2, RoundingMode.DOWN);

        Point point = Point.builder()
                .userId(userId)
                .paymentId(paymentId)
                .title("Payment Point")
                .type(PointType.EARNED)   // ✅ enum 이름과 일치
                .amount(pointAmount)
                .build();

        point = create(point);
        return mapToRes(point);
    }

    /* ================== MANUAL GRANT ================== */

    public PointRes grantManual(PointManualGrantReq req) {
        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        String title = (req.title() == null || req.title().isBlank())
                ? "Studio Point"     // ✅ studio가 준 기본 point
                : req.title().trim();

        Point point = Point.builder()
                .userId(req.userId())
                .paymentId(null)
                .title(title)
                .type(PointType.EARNED)
                .amount(req.amount())
                .build();

        point = create(point);
        return mapToRes(point);
    }

    /* ================== LIST & SUMMARY (Studio/Admin용) ================== */

    @Transactional(readOnly = true)
    public List<PointRes> listByUser(UUID userId) {
        return pointRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToRes)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PointRes> listByPayment(UUID paymentId) {
        return pointRepository.findAllByPaymentIdAndDeletedAtIsNull(paymentId)
                .stream()
                .map(this::mapToRes)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PointRes> listByType(PointType type) {
        return pointRepository.findAllByTypeAndDeletedAtIsNull(type)
                .stream()
                .map(this::mapToRes)
                .toList();
    }

    @Transactional(readOnly = true)
    public PointSummaryRes getSummary(UUID userId) {
        BigDecimal earned = pointRepository.sumEarnedByUser(userId);
        BigDecimal used   = pointRepository.sumUsedByUser(userId);

        if (earned == null) earned = BigDecimal.ZERO;
        if (used   == null) used   = BigDecimal.ZERO;

        BigDecimal balance = earned.subtract(used);
        return new PointSummaryRes(earned, used, balance);
    }



    @Transactional(readOnly = true)
    public PointActiveSummaryRes getActiveSummary(UUID userId) {
        BigDecimal earned = pointRepository.sumEarnedByUser(userId);
        BigDecimal used   = pointRepository.sumUsedByUser(userId);

        if (earned == null) earned = BigDecimal.ZERO;
        if (used   == null) used   = BigDecimal.ZERO;

        BigDecimal active = earned.subtract(used);
        return new PointActiveSummaryRes(active);
    }


    @Transactional(readOnly = true)
    public List<PointRes> filterPoints(PointFilterReq req) {
        if (req.userId() == null) {
            throw new IllegalArgumentException("userId is required for filtering");
        }

        List<Point> base = pointRepository
                .findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(req.userId());

        return base.stream()
                .filter(p -> req.type() == null || p.getType() == req.type())
                .filter(p -> {
                    Instant c = p.getCreatedAt();
                    if (req.from() != null && c.isBefore(req.from())) return false;
                    if (req.to() != null && c.isAfter(req.to())) return false;
                    return true;
                })
                .map(this::mapToRes)
                .toList();
    }
}
