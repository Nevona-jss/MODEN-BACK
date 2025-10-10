package com.moden.modenapi.modules.notify.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.notify.dto.NotificationRes;
import com.moden.modenapi.modules.notify.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller managing system/user notifications.
 */
@Tag(name = "Notification", description = "Notification APIs for user messages, alerts, and announcements.")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    /**
     * 🔹 Retrieve all notifications (admin use)
     */
    @Operation(summary = "List all notifications", description = "Fetch all notifications (admin only).")
    @GetMapping
    public ResponseEntity<ResponseMessage<List<NotificationRes>>> getAll() {
        var data = service.getAll().stream()
                .map(NotificationRes::from)
                .toList();
        return ResponseEntity.ok(ResponseMessage.success("All notifications retrieved successfully", data));
    }

    /**
     * 🔹 Retrieve notifications for a specific user
     */
    @Operation(summary = "List notifications by user", description = "Fetch all notifications for a specific user.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ResponseMessage<List<NotificationRes>>> getByUser(@PathVariable UUID userId) {
        var data = service.listByUser(userId);
        return ResponseEntity.ok(ResponseMessage.success("User notifications retrieved successfully", data));
    }
}
