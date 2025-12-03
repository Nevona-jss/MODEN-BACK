package com.moden.modenapi.modules.event.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.service.ImageUploadService;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.event.dto.EventCreateReq;
import com.moden.modenapi.modules.event.dto.EventRes;
import com.moden.modenapi.modules.event.dto.EventUpdateReq;
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

@Tag(name = "EVENT")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final ImageUploadService imageUploadService;

    /* ================= LIST ================= */

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER','CUSTOMER')")
    @Operation(summary = "현재 스튜디오 이벤트 목록 + 필터 (title, fromDate, toDate)")
    @GetMapping("/list")
    public ResponseEntity<ResponseMessage<List<EventRes>>> listForCurrentStudio(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        UUID currentUserId = CurrentUserUtil.currentUserId();
        // ✅ 항상 studioUserId 로 resolve (studio / designer / customer 공용)
        UUID studioId = eventService.resolveStudioIdForCurrentActor(currentUserId);

        List<EventRes> events = eventService.searchForStudio(
                studioId,
                keyword,
                fromDate,
                toDate
        );

        return ResponseEntity.ok(
                ResponseMessage.success("Event list retrieved", events)
        );
    }

    /* ================= GET ONE ================= */

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER','CUSTOMER')")
    @Operation(summary = "Get event details by ID (current studio)")
    @GetMapping("/get/{eventId}")
    public ResponseEntity<ResponseMessage<EventRes>> getEvent(@PathVariable UUID eventId) {

        UUID currentUserId = CurrentUserUtil.currentUserId();
        UUID studioId = eventService.resolveStudioIdForCurrentActor(currentUserId);

        EventRes res = eventService.getEventForStudio(studioId, eventId);

        return ResponseEntity.ok(ResponseMessage.success("Event retrieved", res));
    }

    /* ================= CREATE ================= */

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Create new event (current salon, with image upload)")
    @PostMapping(
            value = "/create",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
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
                    schema = @Schema(type = "string", format = "date", example = "2025-11-30")
            )
            @RequestParam("endDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(value = "image", required = false) MultipartFile imageFile
    ) throws Exception {

        UUID currentUserId = CurrentUserUtil.currentUserId();
        UUID studioId = eventService.resolveStudioIdForCurrentActor(currentUserId);

        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = imageUploadService.uploadEventImage(imageFile);
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

    /* ================= UPDATE ================= */

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Update existing event (current salon, with image upload)")
    @PatchMapping(
            value = "/update/{eventId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResponseMessage<EventRes>> updateEvent(
            @PathVariable UUID eventId,

            @RequestParam(value = "title", required = false)
            String title,

            @RequestParam(value = "description", required = false)
            String description,

            @RequestParam(value = "discount", required = false)
            BigDecimal discount,

            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(value = "image", required = false)
            MultipartFile imageFile
    ) throws Exception {

        UUID currentUserId = CurrentUserUtil.currentUserId();
        UUID studioId = eventService.resolveStudioIdForCurrentActor(currentUserId);

        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = imageUploadService.uploadEventImage(imageFile);
        }

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

    /* ================= DELETE ================= */

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER','CUSTOMER')")
    @Operation(summary = "Delete event by ID (current studio)")
    @DeleteMapping("/delete/{eventId}")
    public ResponseEntity<ResponseMessage<Void>> deleteEvent(@PathVariable UUID eventId) {

        UUID currentUserId = CurrentUserUtil.currentUserId();
        UUID studioId = eventService.resolveStudioIdForCurrentActor(currentUserId);

        eventService.deleteEventForStudio(studioId, eventId);

        return ResponseEntity.ok(
                ResponseMessage.success("Event deleted", null)
        );
    }
}
