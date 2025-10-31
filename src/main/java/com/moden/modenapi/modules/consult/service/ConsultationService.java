package com.moden.modenapi.modules.consult.service;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.common.service.FileStorageService;
import com.moden.modenapi.modules.consult.dto.*;
import com.moden.modenapi.modules.consult.model.Consultation;
import com.moden.modenapi.modules.consult.repository.ConsultationRepository;
import com.moden.modenapi.modules.payment.repository.PaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationService extends BaseService<Consultation> {

    private final ConsultationRepository consultationRepository;
    private final PaymentRepository paymentRepository;
    private final FileStorageService fileStorageService;


    @Override
    protected ConsultationRepository getRepository() {
        return consultationRepository;
    }
    // ----------------------------------------------------------------------
    // 🔹 UPLOAD IMAGES
    // ----------------------------------------------------------------------
    @Operation(summary = "Upload consultation images (style / before / after)")
    @PostMapping("/{consultationId}/upload")
    public ResponseEntity<ResponseMessage<Map<String, String>>> uploadConsultationImages(
            @PathVariable UUID consultationId,
            @RequestParam(required = false) MultipartFile style,
            @RequestParam(required = false) MultipartFile before,
            @RequestParam(required = false) MultipartFile after
    ) {
        Map<String, String> uploaded = new HashMap<>();
        if (style != null && !style.isEmpty()) uploaded.put("styleImageUrl", fileStorageService.saveFile(style));
        if (before != null && !before.isEmpty()) uploaded.put("beforeImageUrl", fileStorageService.saveFile(before));
        if (after != null && !after.isEmpty()) uploaded.put("afterImageUrl", fileStorageService.saveFile(after));

        return ResponseEntity.ok(ResponseMessage.success("Images uploaded successfully", uploaded));
    }

    // Create
    public ConsultationRes create(ConsultationCreateReq req) {
        if (req.serviceId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "serviceId is required");

        Consultation c = Consultation.builder()
                .serviceId(req.serviceId())
                .status(Optional.ofNullable(req.status()).orElse(ConsultationStatus.PENDING))
                .styleImageUrl(req.styleImageUrl())
                .beforeImageUrl(req.beforeImageUrl())
                .afterImageUrl(req.afterImageUrl())
                .consultationMemo(req.consultationMemo())
                .customerMemo(req.customerMemo())
                .drawingMemoUrl(req.drawingMemoUrl())
                .build();

        create(c);
        return toRes(c);
    }

    // Update
    public ConsultationRes update(UUID id, ConsultationUpdateReq req) {
        Consultation c = consultationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found"));

        if (req.status() != null) c.setStatus(req.status());
        if (req.styleImageUrl() != null) c.setStyleImageUrl(req.styleImageUrl());
        if (req.beforeImageUrl() != null) c.setBeforeImageUrl(req.beforeImageUrl());
        if (req.afterImageUrl() != null) c.setAfterImageUrl(req.afterImageUrl());
        if (req.consultationMemo() != null) c.setConsultationMemo(req.consultationMemo());
        if (req.customerMemo() != null) c.setCustomerMemo(req.customerMemo());
        if (req.drawingMemoUrl() != null) c.setDrawingMemoUrl(req.drawingMemoUrl());

        c.setUpdatedAt(Instant.now());
        update(c);

        return toRes(c);
    }

    // Read
    @Transactional(readOnly = true)
    public ConsultationRes get(UUID id) {
        Consultation c = consultationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found"));
        return toRes(c);
    }

    @Transactional(readOnly = true)
    public List<ConsultationRes> listByService(UUID serviceId) {
        return consultationRepository.findAllByServiceIdAndDeletedAtIsNull(serviceId)
                .stream().map(this::toRes).toList();
    }

    // Delete
    public void softDelete(UUID id) {
        Consultation c = consultationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found"));
        c.setDeletedAt(Instant.now());
        update(c);
    }

    // Mapper
    private ConsultationRes toRes(Consultation c) {
        var paymentOpt = paymentRepository.findTopByServiceIdAndDeletedAtIsNullOrderByCreatedAtDesc(c.getServiceId());
        var payment = paymentOpt.orElse(null);

        return new ConsultationRes(
                c.getId(),
                c.getServiceId(),
                c.getStatus(),
                c.getStyleImageUrl(),
                c.getBeforeImageUrl(),
                c.getAfterImageUrl(),
                c.getConsultationMemo(),
                c.getCustomerMemo(),
                c.getDrawingMemoUrl(),

                // ✅ Payment Info
                payment != null ? payment.getPaymentStatus() : null,
                payment != null ? payment.getPaymentMethod() : null,
                payment != null && payment.getAmount() != null
                        ? payment.getAmount().longValue()
                        : null,
                payment != null ? payment.getCreatedAt() : null,  // ✅ Use createdAt

                // ✅ Consultation timestamps
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

}
