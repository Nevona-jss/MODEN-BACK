package com.moden.modenapi.modules.designer.controller;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.consultation.dto.ConsultationRes;
import com.moden.modenapi.modules.consultation.service.ConsultationService;
import com.moden.modenapi.modules.customer.dto.CustomerSignUpRequest;
import com.moden.modenapi.modules.customer.service.CustomerService;
import com.moden.modenapi.modules.designer.dto.*;
import com.moden.modenapi.modules.designer.service.DesignerService;
import com.moden.modenapi.modules.reservation.dto.ReservationResponse;
import com.moden.modenapi.modules.reservation.dto.ReservationUpdateRequest;
import com.moden.modenapi.modules.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "DESIGNER")
@RestController
@RequestMapping("/api/designers")
@RequiredArgsConstructor
public class DesignerController {

    private final DesignerService designerService;
    private final CustomerService customerService;
    private final ReservationService reservationService;
    private final ConsultationService consultationService;

    // ----------------------------------------------------------------------
    // CUSTOMER register (already existed)
    // ----------------------------------------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @PostMapping("/customers/register")
    public ResponseEntity<ResponseMessage<Void>> registerCustomer(@RequestBody CustomerSignUpRequest req) {
        customerService.customerRegister(req, "default123!");
        return ResponseEntity.ok(ResponseMessage.<Void>builder()
                .success(true)
                .message("Customer registered (studio/designer auto-assigned).")
                .build());
    }

    // ----------------------------------------------------------------------
    // GET portfolio (already existed)
    // ----------------------------------------------------------------------
    @GetMapping("/{id}/portfolio")
    public ResponseEntity<ResponseMessage<List<PortfolioItemRes>>> getPortfolio(
            @PathVariable("id") UUID designerId
    ) {
        var list = designerService.getPortfolio(designerId);
        return ResponseEntity.ok(ResponseMessage.success("OK", list));
    }


    @PreAuthorize("hasRole('DESIGNER')")
    @Operation(summary = "Update my profile (Designer self-update)")
    @PatchMapping(
            value = "/profile",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResponseMessage<DesignerResponse>> updateProfile(
            HttpServletRequest request,
            @RequestBody DesignerUpdateReq req
    ) {
        var updated = designerService.updateProfile(request, req);
        return ResponseEntity.ok(
                ResponseMessage.success("Designer profile updated", updated)
        );
    }

    @PreAuthorize("hasRole('DESIGNER')")
    @Operation(summary = "Designer o'zi ishlaydigan reservationni o'zgartirishi")
    @PutMapping("/reservations/update/{id}")
    public ResponseEntity<ResponseMessage<ReservationResponse>> updateByDesigner(
            @PathVariable UUID id,
            @RequestBody ReservationUpdateRequest request
    ) {
        UUID designerId = CurrentUserUtil.currentUserId();
        ReservationResponse response = reservationService.updateByDesigner(designerId, id, request);
        return ResponseEntity.ok(ResponseMessage.success("Designer reservation updated.", response));
    }

    @PreAuthorize("hasRole('DESIGNER')")
    @Operation(summary = "Designer o'zi ishlaydigan reservationni bekor qilishi")
    @PostMapping("/reservations/{id}/cancel")
    public ResponseEntity<ResponseMessage<ReservationResponse>> cancelByDesigner(
            @PathVariable UUID id
    ) {
        UUID designerId = CurrentUserUtil.currentUserId();
        ReservationResponse response = reservationService.cancelByDesigner(designerId, id);
        return ResponseEntity.ok(ResponseMessage.success("Designer reservation canceled.", response));
    }


    @PreAuthorize("hasRole('DESIGNER')")
    @Operation(summary = "Designerga tegishli barcha reservationlar")
    @GetMapping("/reservations/list")
    public ResponseEntity<ResponseMessage<List<ReservationResponse>>> listReservations() {
        UUID designerId = CurrentUserUtil.currentUserId();
        List<ReservationResponse> list = reservationService.listForDesignerAll(designerId);
        return ResponseEntity.ok(ResponseMessage.success("Designer reservations fetched.", list));
    }

    @PreAuthorize("hasRole('DESIGNER')")
    @Operation(summary = "Designer reservationlari (status bo‘yicha)")
    @GetMapping("/reservations/list/status")
    public ResponseEntity<ResponseMessage<List<ReservationResponse>>> listReservationsByStatus(
            @RequestParam ReservationStatus status
    ) {
        UUID designerId = CurrentUserUtil.currentUserId();
        List<ReservationResponse> list = reservationService.listForDesignerByStatus(designerId, status);
        return ResponseEntity.ok(ResponseMessage.success("Designer reservations by status fetched.", list));
    }

    @PreAuthorize("hasRole('DESIGNER')")
    @Operation(summary = "Dizaynerning ma'lum vaqt oralig'idagi reservationlari")
    @GetMapping("/reservations/range")
    public ResponseEntity<ResponseMessage<List<ReservationResponse>>> listRangedReservations(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        UUID designerId = CurrentUserUtil.currentUserId();
        List<ReservationResponse> list = reservationService.listForDesignerRange(designerId, from, to);
        return ResponseEntity.ok(ResponseMessage.success("Designer reservations in range fetched.", list));
    }

    @PreAuthorize("hasRole('DESIGNER')")
    @Operation(summary = "Current designer – kunlik reservationlar")
    @GetMapping("/reservations/daily")
    public ResponseEntity<ResponseMessage<List<ReservationResponse>>> listDailyReservations(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date   // misol: 2025-11-20
    ) {
        UUID designerId = CurrentUserUtil.currentUserId();
        List<ReservationResponse> list = reservationService.listForDesignerDaily(designerId, date);
        return ResponseEntity.ok(ResponseMessage.success("Designer daily reservations fetched.", list));
    }

    @PreAuthorize("hasRole('DESIGNER')")
    @Operation(summary = "Current designer – haftalik reservationlar")
    @GetMapping("/reservations/weekly")
    public ResponseEntity<ResponseMessage<List<ReservationResponse>>> listWeeklyReservations(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate anyDateInWeek   // shu haftadagi har qanday sana
    ) {
        UUID designerId = CurrentUserUtil.currentUserId();
        List<ReservationResponse> list = reservationService.listForDesignerWeekly(designerId, anyDateInWeek);
        return ResponseEntity.ok(ResponseMessage.success("Designer weekly reservations fetched.", list));
    }

    @PreAuthorize("hasRole('DESIGNER')")
    @Operation(summary = "Current designer – oylik reservationlar")
    @GetMapping("/reservations/monthly")
    public ResponseEntity<ResponseMessage<List<ReservationResponse>>> listMonthlyReservations(
            @RequestParam int year,   // 2025
            @RequestParam int month   // 1~12
    ) {
        UUID designerId = CurrentUserUtil.currentUserId();
        List<ReservationResponse> list = reservationService.listForDesignerMonthly(designerId, year, month);
        return ResponseEntity.ok(ResponseMessage.success("Designer monthly reservations fetched.", list));
    }

    // ----------------------------------------
    //  디자이너 본인 상담 목록 (디자이너/스튜디오용)
    // ----------------------------------------
    @PreAuthorize("hasAnyRole('DESIGNER')")
    @Operation(
            summary = "디자이너 본인 상담 목록 조회",
            description = "현재 로그인한 디자이너에게 배정된 예약들을 기준으로 상담 목록을 조회합니다."
    )
    @GetMapping("/consultation/list")
    public ResponseEntity<ResponseMessage<List<ConsultationRes>>> listForDesignerMe() {
        UUID designerId = CurrentUserUtil.currentUserId();
        List<ConsultationRes> list = consultationService.listForDesigner(designerId);
        return ResponseEntity.ok(ResponseMessage.success("디자이너 상담 목록 조회가 완료되었습니다.", list));
    }

}
