package com.moden.modenapi.modules.reservation.controller;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.reservation.dto.ReservationCreateRequest;
import com.moden.modenapi.modules.reservation.dto.ReservationResponse;
import com.moden.modenapi.modules.reservation.dto.ReservationUpdateRequest;
import com.moden.modenapi.modules.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "예약 생성")
    @PostMapping("/create")
    public ResponseEntity<ResponseMessage<ReservationResponse>> create(
            @RequestBody ReservationCreateRequest request
    ) {
        UUID currentUserId = CurrentUserUtil.currentUserId();
        ReservationResponse response = reservationService.createForCustomer(currentUserId, request);
        return ResponseEntity.ok(
                ResponseMessage.success("예약이 생성되었습니다.", response)
        );
    }

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

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "예약 전체 목록 조회")
    @GetMapping("/list")
    public ResponseEntity<ResponseMessage<List<ReservationResponse>>> list() {
        List<ReservationResponse> list = reservationService.list();
        return ResponseEntity.ok(
                ResponseMessage.success("예약 목록 조회가 완료되었습니다.", list)
        );
    }


    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "예약 수정")
    @PatchMapping("/update/{id}")
    public ResponseEntity<ResponseMessage<ReservationResponse>> update(
            @PathVariable UUID id,
            @RequestBody ReservationUpdateRequest request
    ) {
        ReservationResponse response = reservationService.update(id, request);
        return ResponseEntity.ok(
                ResponseMessage.success("예약 수정이 완료되었습니다.", response)
        );
    }

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "예약 취소 (상태 자동 CANCELED)")
    @PostMapping("/cancel/{id}")
    public ResponseEntity<ResponseMessage<ReservationResponse>> cancel(
            @PathVariable UUID id
    ) {
        ReservationResponse response = reservationService.cancel(id);
        return ResponseEntity.ok(
                ResponseMessage.success("예약 취소가 완료되었습니다.", response)
        );
    }


    @Operation(summary = "특정 디자이너의 모든 예약 조회")
    @GetMapping("/designer/{designerId}")
    public ResponseEntity<ResponseMessage<List<ReservationResponse>>> listByDesigner(
            @PathVariable UUID designerId
    ) {
        List<ReservationResponse> list = reservationService.listByDesigner(designerId);
        return ResponseEntity.ok(ResponseMessage.success("디자이너 예약 목록 조회가 완료되었습니다.", list));
    }


    // ---------- 상태별 필터 ----------
    @Operation(summary = "상태별 예약 조회 (RESERVED / CANCELED)")
    @GetMapping("/filter/status")
    public ResponseEntity<ResponseMessage<List<ReservationResponse>>> listByStatus(
            @RequestParam ReservationStatus status
    ) {
        List<ReservationResponse> list = reservationService.listByStatus(status);
        return ResponseEntity.ok(ResponseMessage.success("상태별 예약 조회가 완료되었습니다.", list));
    }

    // ---------- 일간 필터 ----------
    @Operation(summary = "일간 전체 예약 조회 (스튜디오 전체 기준)")
    @GetMapping("/filter/daily")
    public ResponseEntity<ResponseMessage<List<ReservationResponse>>> listDaily(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date    // 예: 2025-11-20
    ) {
        List<ReservationResponse> list = reservationService.listDaily(date);
        return ResponseEntity.ok(ResponseMessage.success("일간 예약 조회가 완료되었습니다.", list));
    }

    // ---------- 주간 필터 ----------
    @Operation(summary = "주간 전체 예약 조회 (해당 날짜가 포함된 주 기준)")
    @GetMapping("/filter/weekly")
    public ResponseEntity<ResponseMessage<List<ReservationResponse>>> listWeekly(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date    // 해당 주에 포함된 임의의 날짜
    ) {
        List<ReservationResponse> list = reservationService.listWeekly(date);
        return ResponseEntity.ok(ResponseMessage.success("주간 예약 조회가 완료되었습니다.", list));
    }

    // ---------- 월간 필터 ----------
    @Operation(summary = "월간 전체 예약 조회 (year + month)")
    @GetMapping("/filter/monthly")
    public ResponseEntity<ResponseMessage<List<ReservationResponse>>> listMonthly(
            @RequestParam int year,   // 예: 2025
            @RequestParam int month   // 1~12
    ) {
        List<ReservationResponse> list = reservationService.listMonthly(year, month);
        return ResponseEntity.ok(ResponseMessage.success("월간 예약 조회가 완료되었습니다.", list));
    }
}
