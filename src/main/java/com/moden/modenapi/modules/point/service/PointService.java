package com.moden.modenapi.modules.point.service;

import com.moden.modenapi.common.enums.PointType;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.payment.model.Payment;
import com.moden.modenapi.modules.payment.repository.PaymentRepository;
import com.moden.modenapi.modules.point.dto.PointFilterReq;
import com.moden.modenapi.modules.point.dto.PointRes;
import com.moden.modenapi.modules.point.model.Point;
import com.moden.modenapi.modules.point.model.StudioPointPolicy;
import com.moden.modenapi.modules.point.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
public class PointService extends BaseService<Point> {

    private final PointRepository pointRepository;
    private final PaymentRepository paymentRepository;
    private final StudioPointPolicyService studioPointPolicyService;

    @Override
    protected PointRepository getRepository() {
        return pointRepository;
    }


    // ----------------------------------------------------------------------
    // 🔹 Auto create after successful payment
    // ----------------------------------------------------------------------
    public PointRes create(UUID paymentId) {
        // ✅ fetch payment
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        UUID serviceId = payment.getServiceId();
        if (serviceId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment has no linked service");

        // ✅ fetch studio’s cashback policy
        StudioPointPolicy policy = studioPointPolicyService.getPolicyByService(serviceId);
        if (policy == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No cashback policy found for this studio");

        BigDecimal rate = policy.getPointRate();
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Studio’s point rate must be greater than 0");

        // ✅ calculate earned points
        BigDecimal earnedPoints = payment.getAmount()
                .multiply(rate)
                .divide(BigDecimal.valueOf(100));

        if (earnedPoints.compareTo(BigDecimal.ZERO) <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Calculated points must be greater than zero");

        // ✅ save new point record
        Point p = Point.builder()
                .paymentId(paymentId)
                .type(PointType.EARNED)
                .amount(earnedPoints)
                .build();

        create(p);
        return toRes(p);
    }

    // ----------------------------------------------------------------------
    // 🔹 READ
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public PointRes get(UUID id) {
        Point p = pointRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Point record not found"));
        return toRes(p);
    }

    @Transactional(readOnly = true)
    public List<PointRes> listByPayment(UUID paymentId) {
        return pointRepository.findAllByPaymentIdAndDeletedAtIsNull(paymentId)
                .stream().map(this::toRes).toList();
    }

    @Transactional(readOnly = true)
    public List<PointRes> listByType(PointType type) {
        return pointRepository.findAllByTypeAndDeletedAtIsNull(type)
                .stream().map(this::toRes).toList();
    }

    // ----------------------------------------------------------------------
    // 🔹 Soft delete
    // ----------------------------------------------------------------------
    public void softDelete(UUID id) {
        Point p = pointRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Point record not found"));
        p.setDeletedAt(Instant.now());
        update(p);
    }

    // ----------------------------------------------------------------------
    // 🔹 Mapper
    // ----------------------------------------------------------------------
    private PointRes toRes(Point p) {
        return new PointRes(
                p.getId(),
                p.getPaymentId(),
                p.getType(),
                p.getAmount(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }


    @Transactional(readOnly = true)
    public List<PointRes> filterPoints(PointFilterReq req) {
        if (req.userId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");

        // Base list
        List<Point> points = pointRepository.findAllByUserIdAndDeletedAtIsNull(req.userId());

        // Filter by type (적립 / 사용)
        if (req.type() != null) {
            points = points.stream()
                    .filter(p -> p.getType() == req.type())
                    .toList();
        }

        // Filter by date
        LocalDate today = LocalDate.now();
        Instant startDate = null;

        switch (req.dateRange() != null ? req.dateRange().toLowerCase() : "") {
            case "today" -> startDate = today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
            case "week"  -> startDate = today.minusDays(7).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
            case "month" -> startDate = today.minusDays(30).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        }

        if (startDate != null) {
            final Instant finalStartDate = startDate; // ✅ Fix here
            points = points.stream()
                    .filter(p -> p.getCreatedAt().isAfter(finalStartDate))
                    .toList();
        }

        return points.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toRes)
                .toList();
    }


}
