package com.moden.modenapi.modules.event.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.event.dto.*;
import com.moden.modenapi.modules.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Event", description = "Salon Event management APIs")
@RestController
@RequestMapping("/api/studios/{studioId}/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // 🔹 GET ALL
    @Operation(summary = "Get all events for a specific salon (studio)")
    @GetMapping
    public ResponseEntity<ResponseMessage<List<EventRes>>> getAllByStudio(@PathVariable UUID studioId) {
        List<EventRes> events = eventService.getAllByStudio(studioId);
        return ResponseEntity.ok(ResponseMessage.success("Event list retrieved", events));
    }

    // 🔹 GET ONE
    @Operation(summary = "Get event details by ID")
    @GetMapping("/{eventId}")
    public ResponseEntity<ResponseMessage<EventRes>> getEvent(@PathVariable UUID eventId) {
        EventRes event = eventService.getEvent(eventId);
        return ResponseEntity.ok(ResponseMessage.success("Event retrieved", event));
    }

    // 🔹 CREATE
    @Operation(summary = "Create new event (Salon only)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ResponseMessage<EventRes>> createEvent(
            @PathVariable UUID studioId,
            @RequestBody EventCreateReq req
    ) {
        EventRes created = eventService.createEvent(studioId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Event created successfully", created));
    }

    // 🔹 UPDATE
    @Operation(summary = "Update existing event (Salon only)")
    @PatchMapping("/{eventId}")
    public ResponseEntity<ResponseMessage<EventRes>> updateEvent(
            @PathVariable UUID eventId,
            @RequestBody EventUpdateReq req
    ) {
        EventRes updated = eventService.updateEvent(eventId, req);
        return ResponseEntity.ok(ResponseMessage.success("Event updated successfully", updated));
    }

    // 🔹 DELETE
    @Operation(summary = "Delete (soft delete) event by ID (Salon only)")
    @DeleteMapping("/{eventId}")
    public ResponseEntity<ResponseMessage<Void>> deleteEvent(@PathVariable UUID eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.ok(ResponseMessage.success("Event deleted successfully", null));
    }
}
