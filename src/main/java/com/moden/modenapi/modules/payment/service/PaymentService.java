package com.moden.modenapi.modules.payment.service;

import com.moden.modenapi.common.enums.PaymentStatus;
import com.moden.modenapi.common.model.BaseEntity;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.payment.dto.*;
import com.moden.modenapi.modules.payment.model.Payment;
import com.moden.modenapi.modules.payment.repository.PaymentRepository;
import com.moden.modenapi.modules.studioservice.model.ServiceUsedProduct;
import com.moden.modenapi.modules.studioservice.repository.ServiceUsedProductRepository;
import com.moden.modenapi.modules.studioservice.repository.StudioServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService extends BaseService<Payment> {

    private final PaymentRepository paymentRepository;
    private final StudioServiceRepository studioServiceRepository;
    private final ServiceUsedProductRepository usedProductRepository;

    @Override
    protected PaymentRepository getRepository() {
        return paymentRepository;
    }

    // ----------------------------------------------------------------------
    // CREATE
    // ----------------------------------------------------------------------
    public PaymentRes create(PaymentCreateReq req) {
        if (req.serviceId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "serviceId is required");

        // 1️⃣ Get service price
        var service = studioServiceRepository.findById(req.serviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        var servicePrice = service.getServicePrice();

        // 2️⃣ Sum used products
        var products = usedProductRepository.findAllByServiceId(req.serviceId());
        var productTotal = products.stream()
                .map(ServiceUsedProduct::getTotalPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3️⃣ Apply discounts
        var coupon = Optional.ofNullable(req.couponDiscount()).orElse(BigDecimal.ZERO);
        var points = Optional.ofNullable(req.pointsUsed()).orElse(BigDecimal.ZERO);

        // 4️⃣ Calculate final amount
        var amount = servicePrice.add(productTotal).subtract(coupon).subtract(points);
        if (amount.compareTo(BigDecimal.ZERO) < 0) amount = BigDecimal.ZERO;

        // 5️⃣ Save payment
        var payment = Payment.builder()
                .serviceId(req.serviceId())
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentMethod(req.paymentMethod())
                .couponDiscount(coupon)
                .pointsUsed(points)
                .amount(amount)
                .build();

        create(payment);
        return toRes(payment, servicePrice, productTotal);
    }

    // ----------------------------------------------------------------------
    // UPDATE
    // ----------------------------------------------------------------------
    public PaymentRes update(UUID id, PaymentUpdateReq req) {
        var p = paymentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (req.paymentStatus() != null) p.setPaymentStatus(req.paymentStatus());
        if (req.paymentMethod() != null) p.setPaymentMethod(req.paymentMethod());
        if (req.couponDiscount() != null) p.setCouponDiscount(req.couponDiscount());
        if (req.pointsUsed() != null) p.setPointsUsed(req.pointsUsed());

        p.setUpdatedAt(Instant.now());
        update(p);

        // dynamic totals
        var service = studioServiceRepository.findById(p.getServiceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        var products = usedProductRepository.findAllByServiceId(p.getServiceId());
        var productTotal = products.stream()
                .map(ServiceUsedProduct::getTotalPrice)
                .filter(x -> x != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return toRes(p, service.getServicePrice(), productTotal);
    }

    // ----------------------------------------------------------------------
    // READ
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public PaymentRes get(UUID id) {
        var p = paymentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        var service = studioServiceRepository.findById(p.getServiceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        var productTotal = usedProductRepository.findAllByServiceId(p.getServiceId())
                .stream().map(ServiceUsedProduct::getTotalPrice)
                .filter(x -> x != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return toRes(p, service.getServicePrice(), productTotal);
    }

    @Transactional(readOnly = true)
    public PaymentRes getByService(UUID serviceId) {
        var p = paymentRepository.findByServiceIdAndDeletedAtIsNull(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        var service = studioServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        var productTotal = usedProductRepository.findAllByServiceId(serviceId)
                .stream().map(ServiceUsedProduct::getTotalPrice)
                .filter(x -> x != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return toRes(p, service.getServicePrice(), productTotal);
    }

    @Transactional(readOnly = true)
    public List<PaymentRes> listByStatus(PaymentStatus status) {
        return paymentRepository.findAllByPaymentStatusAndDeletedAtIsNull(status)
                .stream()
                .map(p -> {
                    var service = studioServiceRepository.findById(p.getServiceId()).orElse(null);
                    var price = service != null ? service.getServicePrice() : BigDecimal.ZERO;
                    var productTotal = usedProductRepository.findAllByServiceId(p.getServiceId())
                            .stream().map(ServiceUsedProduct::getTotalPrice)
                            .filter(x -> x != null)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return toRes(p, price, productTotal);
                })
                .toList();
    }

    // ----------------------------------------------------------------------
    // DELETE (Soft)
    // ----------------------------------------------------------------------
    public void softDelete(UUID id) {
        var p = paymentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        p.setDeletedAt(Instant.now());
        update(p);
    }

    // ----------------------------------------------------------------------
    // MAPPER
    // ----------------------------------------------------------------------
    private PaymentRes toRes(Payment p, BigDecimal servicePrice, BigDecimal productTotal) {
        return new PaymentRes(
                p.getId(),
                p.getServiceId(),
                p.getPaymentStatus(),
                p.getPaymentMethod(),
                servicePrice,
                productTotal,
                p.getCouponDiscount(),
                p.getPointsUsed(),
                p.getAmount(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
