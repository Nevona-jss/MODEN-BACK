package com.moden.modenapi.modules.booking.controller;

import com.moden.modenapi.common.response.ResponseMessage;


import com.moden.modenapi.modules.booking.dto.ReservationCreateReq;
import com.moden.modenapi.modules.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller that manages reservation creation and listing.
 * <p>
 * Accessible by both Customer and Designer roles.
 */
@Tag(name = "Reservation", description = "Booking and reservation APIs")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * Creates a new reservation between customer and designer.
     *
     * @param req reservation details
     * @return created reservation object
     */
    @Operation(summary = "Create reservation")
    @PostMapping("/create")
    public ResponseEntity<ResponseMessage<?>> create(@RequestBody ReservationCreateReq req) {
        var data = reservationService.createReservation(req);
        return ResponseEntity.ok(
                ResponseMessage.builder()
                        .success(true)
                        .message("Reservation created successfully")
                        .data(data)
                        .build()
        );
    }

    /**
     * Retrieves all existing reservations (admin only).
     *
     * @return reservation list
     */
    @Operation(summary = "List all reservations", description = "Fetch all reservations in the system.")
    @GetMapping
    public ResponseEntity<ResponseMessage<?>> getAll() {
        var list = reservationService.getAll();
        return ResponseEntity.ok(
                ResponseMessage.builder()
                        .success(true)
                        .message("Reservations retrieved successfully")
                        .data(list)
                        .build()
        );
    }
}
