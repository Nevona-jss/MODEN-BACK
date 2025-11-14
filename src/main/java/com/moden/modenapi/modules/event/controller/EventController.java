package com.moden.modenapi.modules.event.controller;

import com.moden.modenapi.common.dto.UploadResponse;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.service.ImageUploadService;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.event.dto.*;
import com.moden.modenapi.modules.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "HAIR STUDIO-EVENT")
@RestController
@RequestMapping("/api/studios/events")   // ✅ 더 이상 {studioId} 없음
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final ImageUploadService  imageUploadService;

    // 🔹 GET ALL (현재 로그인한 studio 기준)
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Get all events for current salon (studio)")
    @GetMapping("/list")
    public ResponseEntity<ResponseMessage<List<EventRes>>> getAllForCurrentStudio() {

        UUID studioId = CurrentUserUtil.currentUserId();  // ✅ 로그인된 사용자 → studioId 로 사용
        List<EventRes> events = eventService.getAllByStudio(studioId);

        return ResponseEntity.ok(ResponseMessage.success("Event list retrieved", events));
    }

    // 🔹 GET ONE (현재 studio 권한 체크 포함)
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Get event details by ID (current studio)")
    @GetMapping("/get/{eventId}")
    public ResponseEntity<ResponseMessage<EventRes>> getEvent(@PathVariable UUID eventId) {

        UUID studioId = CurrentUserUtil.currentUserId();
        EventRes event = eventService.getEventForStudio(studioId, eventId);  // ✅ studio check 포함

        return ResponseEntity.ok(ResponseMessage.success("Event retrieved", event));
    }

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Create new event (current salon, with image upload)")
    @PostMapping(
            value = "/create",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE  // ✅ multipart
    )
    public ResponseEntity<ResponseMessage<EventRes>> createEvent(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "discount", required = false) BigDecimal discount,


            @Parameter(
                    description = "Event start date (YYYY-MM-DD)",
                    example = "2025-07-01",
                    schema = @Schema(type = "string", format = "date", example = "2025-07-01")
            )
            @RequestParam("startDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @Parameter(
                    description = "Event end date (YYYY-MM-DD)",
                    example = "2025-11-31",
                    schema = @Schema(type = "string", format = "date", example = "2025-02-31")
            )
            @RequestParam("endDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(value = "image", required = false) MultipartFile imageFile
    ) throws Exception {

        UUID studioId = CurrentUserUtil.currentUserId();

        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            // ✅ oldingi ImageUploadService misolini ishlatyapmiz deb hisoblayman
            UploadResponse uploadRes = imageUploadService.uploadEventImage(imageFile);
            imageUrl = uploadRes.url();   // masalan: /uploads/events/2025/11/14/xxx.jpg
        }

        EventCreateReq req = new EventCreateReq(
                title,
                description,
                imageUrl,
                discount,
                startDate,
                endDate
        );

        EventRes created = eventService.createEvent(studioId, req);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Event created successfully", created));
    }



    // 🔹 UPDATE (multipart, image ham yangilash mumkin)
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Update existing event (current salon, with image upload)")
    @PatchMapping(
            value = "/update/{eventId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE   // ✅ create bilan bir xil
    )
    public ResponseEntity<ResponseMessage<EventRes>> updateEvent(
            @PathVariable UUID eventId,

            @RequestParam(value = "title", required = false)
            @Parameter(description = "Event title", example = "Updated Summer Discount 40%")
            String title,

            @RequestParam(value = "description", required = false)
            @Parameter(description = "Event description", example = "40% off on all coloring services this summer.")
            String description,

            @RequestParam(value = "discount", required = false)
            @Parameter(description = "Discount amount", example = "40000")
            BigDecimal discount,

            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(
                    description = "Event start date (YYYY-MM-DD)",
                    example = "2025-07-10",
                    schema = @Schema(type = "string", format = "date", example = "2025-07-10")
            )
            LocalDate startDate,

            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(
                    description = "Event end date (YYYY-MM-DD)",
                    example = "2025-07-31",
                    schema = @Schema(type = "string", format = "date", example = "2025-07-31")
            )
            LocalDate endDate,

            @RequestParam(value = "image", required = false)
            @Parameter(description = "New event banner image file (optional)", required = false)
            MultipartFile imageFile
    ) throws Exception {

        UUID studioId = CurrentUserUtil.currentUserId();

        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            // ✅ yangi rasm yuklash (events/...) → URL
            UploadResponse uploadRes = imageUploadService.uploadEventImage(imageFile);
            imageUrl = uploadRes.url();   // masalan: /uploads/events/2025/11/14/xxx.jpg
        }

        // 🔹 EventUpdateReq – barcha fieldlar optional (null bo‘lsa service ichida o‘zgarmaydi)
        EventUpdateReq req = new EventUpdateReq(
                title,
                description,
                imageUrl,
                discount,
                startDate,
                endDate
        );

        EventRes updated = eventService.updateEventForStudio(studioId, eventId, req);

        return ResponseEntity.ok(ResponseMessage.success("Event updated successfully", updated));
    }

    // 🔹 DELETE
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Delete (soft delete) event by ID (current salon)")
    @DeleteMapping("/delete/{eventId}")
    public ResponseEntity<ResponseMessage<Void>> deleteEvent(@PathVariable UUID eventId) {

        UUID studioId = CurrentUserUtil.currentUserId();
        eventService.deleteEventForStudio(studioId, eventId);

        return ResponseEntity.ok(ResponseMessage.success("Event deleted successfully", null));
    }
}
