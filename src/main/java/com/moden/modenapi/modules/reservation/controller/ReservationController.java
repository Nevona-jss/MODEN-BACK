package com.moden.modenapi.modules.reservation.controller;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.reservation.dto.ReservationCreateRequest;
import com.moden.modenapi.modules.reservation.dto.ReservationPageRes;
import com.moden.modenapi.modules.reservation.dto.ReservationResponse;
import com.moden.modenapi.modules.reservation.dto.ReservationUpdateRequest;
import com.moden.modenapi.modules.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


@Tag(name = "RESERVATION")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    // ============================================================
    // 1) LIST (filter + pagination)
    // ============================================================
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Reservation list (filter + pagination)")
    @GetMapping("/list")
    public ResponseEntity<ResponseMessage<ReservationPageRes>> listDynamic(
            @RequestParam(required = false) UUID designerId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        var list = reservationService.searchDynamic(
                designerId,
                customerId,
                serviceId,
                status,
                fromDate,
                toDate,
                page,
                size
        );
        return ResponseEntity.ok(
                ResponseMessage.success("Reservation filtered list (paged).", list)
        );
    }

    // ============================================================
    // 2) CREATE
    // ============================================================

    @PostMapping(
            value = "/create",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "예약 생성 (JSON body 기반)")
    public ResponseEntity<ResponseMessage<ReservationResponse>> create(
            @Valid @RequestBody ReservationCreateRequest request
    ) {
        var response = reservationService.createReservation(request);

        return ResponseEntity.ok(
                ResponseMessage.success("예약이 생성되었습니다.", response)
        );
    }


    // ============================================================
    // 3) GET BY ID
    // ============================================================
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "예약 단건 조회 (ID 기준)")
    @GetMapping("/get/{id}")
    public ResponseEntity<ResponseMessage<ReservationResponse>> get(
            @PathVariable UUID id
    ) {
        ReservationResponse response = reservationService.get(id);
        return ResponseEntity.ok(
                ResponseMessage.success("예약 조회가 완료되었습니다.", response)
        );
    }

    // ============================================================
    // 4) UPDATE (부분 수정 가능)
    // ============================================================

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @PatchMapping(
            value = "/update/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "예약 수정 (JSON body 기반, 모든 필드 optional)")
    public ResponseEntity<ResponseMessage<ReservationResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ReservationUpdateRequest request
    ) {
        var response = reservationService.update(id, request);

        return ResponseEntity.ok(
                ResponseMessage.success("예약 수정이 완료되었습니다.", response)
        );
    }

    // ============================================================
    // 5) CANCEL
    // ============================================================
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "예약 취소 (상태 자동 CANCELED)")
    @PatchMapping("/cancel/{id}")
    public ResponseEntity<ResponseMessage<ReservationResponse>> cancel(
            @PathVariable UUID id
    ) {
        ReservationResponse response = reservationService.cancel(id);
        return ResponseEntity.ok(
                ResponseMessage.success("예약 취소가 완료되었습니다.", response)
        );
    }

    // ============================================================
    // 6) DESIGNER BO‘YICHA LIST (simple)
    // ============================================================
    @Operation(summary = "특정 디자이너의 모든 예약 조회")
    @GetMapping("/designer/{designerId}")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    public ResponseEntity<ResponseMessage<List<ReservationResponse>>> listByDesigner(
            @PathVariable UUID designerId
    ) {
        List<ReservationResponse> list = reservationService.listByDesigner(designerId);
        return ResponseEntity.ok(
                ResponseMessage.success("디자이너 예약 목록 조회가 완료되었습니다.", list)
        );
    }

}
