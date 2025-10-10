package com.moden.modenapi.modules.admin;

import com.moden.modenapi.common.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for system administrators (super admins).
 * Accessible only for users with role: ADMIN.
 */
@Tag(name = "Admin", description = "System administration APIs")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "List all users", description = "Fetch the list of all users across roles.")
    @GetMapping("/users")
    public ResponseEntity<ResponseMessage<?>> getAllUsers() {
        var users = adminService.getAllUsers();
        return ResponseEntity.ok(ResponseMessage.success("User list retrieved successfully", users));
    }

    @Operation(summary = "List all reservations", description = "Fetch the list of all reservations in the system.")
    @GetMapping("/reservations")
    public ResponseEntity<ResponseMessage<?>> getAllReservations() {
        var list = adminService.getAllReservations();
        return ResponseEntity.ok(ResponseMessage.success("Reservation list retrieved successfully", list));
    }

    @Operation(summary = "List all studios", description = "Fetch the list of all hair studios in the system.")
    @GetMapping("/studios")
    public ResponseEntity<ResponseMessage<?>> getAllStudios() {
        var list = adminService.getAllStudios();
        return ResponseEntity.ok(ResponseMessage.success("Studio list retrieved successfully", list));
    }
}
