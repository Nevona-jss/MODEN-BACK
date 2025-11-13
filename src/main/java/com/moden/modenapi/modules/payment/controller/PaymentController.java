package com.moden.modenapi.modules.payment.controller;

import com.moden.modenapi.common.enums.PaymentStatus;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.payment.dto.*;
import com.moden.modenapi.modules.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "HAIR STUDIO-PAYMENT")
@RestController
@RequestMapping("/api/studios/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

//    @Operation(summary = "Create a payment (auto-calculates totals)")
//    @PostMapping
//    public ResponseEntity<ResponseMessage<PaymentRes>> create(@RequestBody PaymentCreateReq req) {
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(ResponseMessage.success("Payment created successfully", paymentService.create(req)));
//    }
//
//    @Operation(summary = "Update payment info")
//    @PutMapping("/{paymentId}")
//    public ResponseEntity<ResponseMessage<PaymentRes>> update(@PathVariable UUID paymentId, @RequestBody PaymentUpdateReq req) {
//        return ResponseEntity.ok(ResponseMessage.success("Payment updated successfully", paymentService.update(paymentId, req)));
//    }

//    @Operation(summary = "Get payment by ID")
//    @GetMapping("/{paymentId}")
//    public ResponseEntity<ResponseMessage<PaymentRes>> get(@PathVariable UUID paymentId) {
//        return ResponseEntity.ok(ResponseMessage.success(paymentService.get(paymentId)));
//    }
//
//    @Operation(summary = "Get payment by serviceId")
//    @GetMapping("/service/{serviceId}")
//    public ResponseEntity<ResponseMessage<PaymentRes>> getByService(@PathVariable UUID serviceId) {
//        return ResponseEntity.ok(ResponseMessage.success(paymentService.getByService(serviceId)));
//    }
//
//    @Operation(summary = "List payments by status")
//    @GetMapping("/status/{status}")
//    public ResponseEntity<ResponseMessage<List<PaymentRes>>> listByStatus(@PathVariable PaymentStatus status) {
//        return ResponseEntity.ok(ResponseMessage.success(paymentService.listByStatus(status)));
//    }

    @Operation(summary = "Soft delete payment")
    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID paymentId) {
        paymentService.softDelete(paymentId);
        return ResponseEntity.noContent().build();
    }
}
