package com.moden.modenapi.modules.consult.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.consult.model.Consultation;
import com.moden.modenapi.modules.consult.service.ConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing consultations between customers and designers.
 */
@Tag(name = "Consultation", description = "Customer–Designer consultation APIs")
@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService service;

    @Operation(summary = "List all consultations", description = "Fetch all consultation records (admin only).")
    @GetMapping
    public ResponseEntity<ResponseMessage<List<Consultation>>> getAll() {
        return ResponseEntity.ok(ResponseMessage.success("Consultations retrieved", service.getAll()));
    }

    @Operation(summary = "List consultations by customer")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ResponseMessage<List<Consultation>>> getByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(ResponseMessage.success("Customer consultations retrieved", service.listByCustomer(customerId)));
    }
}
