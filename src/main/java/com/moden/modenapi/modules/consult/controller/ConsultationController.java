package com.moden.modenapi.modules.consult.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.service.FileStorageService;
import com.moden.modenapi.modules.consult.dto.ConsultationCreateReq;
import com.moden.modenapi.modules.consult.dto.ConsultationRes;
import com.moden.modenapi.modules.consult.dto.ConsultationUpdateReq;
import com.moden.modenapi.modules.consult.service.ConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Tag(name = "Consultation", description = "Customer–Designer consultation APIs")
@RestController
@RequestMapping("/api/studios/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;
    private final FileStorageService fileStorageService;

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

        if (style != null && !style.isEmpty())
            uploaded.put("styleImageUrl", fileStorageService.saveFile(style));
        if (before != null && !before.isEmpty())
            uploaded.put("beforeImageUrl", fileStorageService.saveFile(before));
        if (after != null && !after.isEmpty())
            uploaded.put("afterImageUrl", fileStorageService.saveFile(after));

        return ResponseEntity.ok(ResponseMessage.success("Images uploaded successfully", uploaded));
    }

    // ----------------------------------------------------------------------
    // 🔹 CREATE
    // ----------------------------------------------------------------------
    @Operation(summary = "Create a new consultation", description = "Registers a new consultation record linked to a service.")
    @PostMapping
    public ResponseEntity<ConsultationRes> create(@RequestBody ConsultationCreateReq req) {
        ConsultationRes res = consultationService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    // ----------------------------------------------------------------------
    // 🔹 UPDATE
    // ----------------------------------------------------------------------
    @Operation(summary = "Update consultation details", description = "Update consultation memo, images, or status.")
    @PutMapping("/{consultationId}")
    public ResponseEntity<ConsultationRes> update(
            @PathVariable UUID consultationId,
            @RequestBody ConsultationUpdateReq req
    ) {
        ConsultationRes res = consultationService.update(consultationId, req);
        return ResponseEntity.ok(res);
    }

    // ----------------------------------------------------------------------
    // 🔹 GET DETAIL
    // ----------------------------------------------------------------------
    @Operation(summary = "Get consultation detail", description = "Fetch full consultation detail including images and memo.")
    @GetMapping("/{consultationId}")
    public ResponseEntity<ConsultationRes> get(@PathVariable UUID consultationId) {
        ConsultationRes res = consultationService.get(consultationId);
        return ResponseEntity.ok(res);
    }

    // ----------------------------------------------------------------------
    // 🔹 LIST BY SERVICE
    // ----------------------------------------------------------------------
    @Operation(summary = "List all consultations by service", description = "Return all consultations for a given service (studio inferred).")
    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<ConsultationRes>> getByService(@PathVariable UUID serviceId) {
        List<ConsultationRes> list = consultationService.listByService(serviceId);
        return ResponseEntity.ok(list);
    }

    // ----------------------------------------------------------------------
    // 🔹 DELETE (Soft)
    // ----------------------------------------------------------------------
    @Operation(summary = "Delete consultation (soft)", description = "Soft delete a consultation record.")
    @DeleteMapping("/{consultationId}")
    public ResponseEntity<Void> delete(@PathVariable UUID consultationId) {
        consultationService.softDelete(consultationId);
        return ResponseEntity.noContent().build();
    }
}
