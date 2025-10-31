package com.moden.modenapi.modules.studioservice.controller;

import com.moden.modenapi.common.enums.PeriodType;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.service.FileStorageService;
import com.moden.modenapi.modules.studioservice.dto.*;
import com.moden.modenapi.modules.studioservice.service.StudioServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Studio Service", description = "Hair studio reservation and service management")
@RestController
@RequestMapping("/api/studios/{studioId}/services")
@RequiredArgsConstructor
public class StudioServiceController {

    private final StudioServiceService studioServiceService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "Upload image (style / before / after) and return URL")
    @PostMapping("/upload-image")
    public ResponseEntity<ResponseMessage<String>> uploadServiceImage(
            @PathVariable UUID studioId,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            String imageUrl = fileStorageService.saveFile(file);
            return ResponseEntity.ok(ResponseMessage.success("Image uploaded successfully", imageUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ResponseMessage.failure("Image upload failed: " + e.getMessage()));
        }
    }


    @Operation(summary = "Upload multiple service images (before / after / style)")
    @PostMapping("/upload-multiple")
    public ResponseEntity<ResponseMessage<Map<String, String>>> uploadMultiple(
            @PathVariable UUID studioId,
            @RequestParam(value = "before", required = false) MultipartFile before,
            @RequestParam(value = "after", required = false) MultipartFile after,
            @RequestParam(value = "style", required = false) MultipartFile style
    ) {
        Map<String, String> result = new HashMap<>();
        if (before != null) result.put("beforeImageUrl", fileStorageService.saveFile(before));
        if (after != null) result.put("afterImageUrl", fileStorageService.saveFile(after));
        if (style != null) result.put("styleImageUrl", fileStorageService.saveFile(style));
        return ResponseEntity.ok(ResponseMessage.success("Images uploaded", result));
    }




    @Operation(summary = "Create new studio service reservation")
    @PostMapping
    public ResponseEntity<ResponseMessage<StudioServiceRes>> createService(
            @PathVariable UUID studioId,
            @RequestBody StudioServiceCreateReq req
    ) {
        StudioServiceRes created = studioServiceService.createService(studioId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Reservation created successfully", created));
    }

    @Operation(summary = "Update existing studio service reservation")
    @PutMapping("/{serviceId}")
    public ResponseEntity<ResponseMessage<StudioServiceRes>> updateService(
            @PathVariable UUID serviceId,
            @RequestBody StudioServiceUpdateReq req
    ) {
        StudioServiceRes updated = studioServiceService.updateService(serviceId, req);
        return ResponseEntity.ok(ResponseMessage.success("Reservation updated successfully", updated));
    }

    @Operation(summary = "Get detailed info for a reservation")
    @GetMapping("/{serviceId}")
    public ResponseEntity<ResponseMessage<StudioServiceRes>> getService(
            @PathVariable UUID serviceId
    ) {
        StudioServiceRes service = studioServiceService.getService(serviceId);
        return ResponseEntity.ok(ResponseMessage.success("Reservation details retrieved", service));
    }

    @Operation(summary = "Get all reservations for a studio")
    @GetMapping
    public ResponseEntity<ResponseMessage<List<StudioServiceRes>>> getAllByStudio(
            @PathVariable UUID studioId
    ) {
        List<StudioServiceRes> services = studioServiceService.getAllByStudio(studioId);
        return ResponseEntity.ok(ResponseMessage.success("Reservation list retrieved", services));
    }

    @Operation(summary = "Soft delete a studio service reservation")
    @DeleteMapping("/{serviceId}")
    public ResponseEntity<ResponseMessage<Void>> deleteService(
            @PathVariable UUID serviceId
    ) {
        studioServiceService.deleteService(serviceId);
        return ResponseEntity.ok(ResponseMessage.success("Reservation deleted successfully", null));
    }


    @Operation(summary = "Get services by period (DAILY / WEEKLY / MONTHLY / RANGE)")
    @GetMapping("/period")
    public ResponseEntity<ResponseMessage<List<StudioServiceRes>>> getServicesByPeriod(
            @PathVariable UUID studioId,
            @RequestParam PeriodType type,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        List<StudioServiceRes> result;

        switch (type) {
            case DAILY -> {
                if (date == null)
                    throw new IllegalArgumentException("Parameter 'date' is required for DAILY type");
                result = studioServiceService.getDailyServices(studioId, date);
            }
            case WEEKLY -> {
                if (date == null)
                    throw new IllegalArgumentException("Parameter 'date' is required for WEEKLY type");
                result = studioServiceService.getWeeklyServices(studioId, date);
            }
            case MONTHLY -> {
                if (year == null || month == null)
                    throw new IllegalArgumentException("Parameters 'year' and 'month' are required for MONTHLY type");
                result = studioServiceService.getMonthlyServices(studioId, year, month);
            }
            case RANGE -> {
                if (startDate == null || endDate == null)
                    throw new IllegalArgumentException("Parameters 'startDate' and 'endDate' are required for RANGE type");
                result = studioServiceService.getServicesBetween(studioId, startDate, endDate);
            }
            default -> throw new IllegalArgumentException("Unsupported period type");
        }

        return ResponseEntity.ok(ResponseMessage.success("Services retrieved successfully", result));
    }
}
