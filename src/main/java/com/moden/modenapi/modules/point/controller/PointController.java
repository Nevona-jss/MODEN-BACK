package com.moden.modenapi.modules.point.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.point.dto.PointLedgerRes;
import com.moden.modenapi.modules.point.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller that handles loyalty and reward points.
 */
@Tag(name = "Point", description = "Loyalty point system APIs")
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService service;

    @Operation(summary = "Get customer balance")
    @GetMapping("/{customerId}/balance")
    public ResponseEntity<ResponseMessage<Integer>> getBalance(@PathVariable UUID customerId) {
        return ResponseEntity.ok(ResponseMessage.success("Balance retrieved", service.getBalance(customerId)));
    }

    @Operation(summary = "Add points")
    @PostMapping("/{customerId}/add")
    public ResponseEntity<ResponseMessage<Integer>> addPoints(
            @PathVariable UUID customerId,
            @RequestParam int amount,
            @RequestParam String reason
    ) {
        return ResponseEntity.ok(ResponseMessage.success("Points added", service.addPoints(customerId, amount, reason)));
    }

    @Operation(summary = "Deduct points")
    @PostMapping("/{customerId}/deduct")
    public ResponseEntity<ResponseMessage<Integer>> deductPoints(
            @PathVariable UUID customerId,
            @RequestParam int amount,
            @RequestParam String reason
    ) {
        return ResponseEntity.ok(ResponseMessage.success("Points deducted", service.deductPoints(customerId, amount, reason)));
    }

    @Operation(summary = "Get point history")
    @GetMapping("/{customerId}/history")
    public ResponseEntity<ResponseMessage<List<PointLedgerRes>>> getHistory(@PathVariable UUID customerId) {
        return ResponseEntity.ok(ResponseMessage.success("Point history retrieved", service.getHistory(customerId)));
    }
}
