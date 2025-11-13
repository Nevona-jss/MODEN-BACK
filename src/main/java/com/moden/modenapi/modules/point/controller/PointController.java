package com.moden.modenapi.modules.point.controller;

import com.moden.modenapi.common.enums.PointType;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.point.dto.*;
import com.moden.modenapi.modules.point.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Point", description = "Customer points management API")
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

//    @Operation(summary = "Create a point record")
//    @PostMapping
//    public ResponseEntity<ResponseMessage<PointRes>> create(@RequestBody PointCreateReq req) {
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(ResponseMessage.success("Point created successfully", pointService.create(req.paymentId())));
    //}

    @Operation(summary = "Get point by ID")
    @GetMapping("/{pointId}")
    public ResponseEntity<ResponseMessage<PointRes>> get(@PathVariable UUID pointId) {
        return ResponseEntity.ok(ResponseMessage.success(pointService.get(pointId)));
    }

    @Operation(summary = "List all points by payment")
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<ResponseMessage<List<PointRes>>> listByPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(ResponseMessage.success(pointService.listByPayment(paymentId)));
    }

    @Operation(summary = "List all points by type")
    @GetMapping("/type/{type}")
    public ResponseEntity<ResponseMessage<List<PointRes>>> listByType(@PathVariable PointType type) {
        return ResponseEntity.ok(ResponseMessage.success(pointService.listByType(type)));
    }

    @Operation(summary = "Soft delete a point record")
    @DeleteMapping("/{pointId}")
    public ResponseEntity<Void> delete(@PathVariable UUID pointId) {
        pointService.softDelete(pointId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Filter point history", description = "Filter points by date range and transaction type")
    @GetMapping("/filter")
    public ResponseEntity<ResponseMessage<List<PointRes>>> filterPoints(PointFilterReq req) {
        return ResponseEntity.ok(ResponseMessage.success(
                "Filtered point list", pointService.filterPoints(req)
        ));
    }
}
