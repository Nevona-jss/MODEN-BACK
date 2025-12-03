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
    // CUSTOMER register
    // ----------------------------------------------------------------------
    @PreAuthorize("hasRole('DESIGNER')")
    @PostMapping("/customers/register")
    public ResponseEntity<ResponseMessage<Void>> registerCustomer(@RequestBody CustomerSignUpRequest req) {
        customerService.customerRegister(req, "default123!");
        return ResponseEntity.ok(ResponseMessage.<Void>builder()
                .success(true)
                .message("Customer registered (studio/designer auto-assigned).")
                .build());
    }



    // ----------------------------------------------------------------------
    // GET portfolio
    // ----------------------------------------------------------------------
    @GetMapping("/{id}/portfolio")
    public ResponseEntity<ResponseMessage<List<String>>> getPortfolio(
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

//    @PreAuthorize("hasRole('DESIGNER')")
//    @Operation(summary = "Designer o'zi ishlaydigan reservationni o'zgartirishi")
//    @PutMapping("/reservations/update/{id}")
//    public ResponseEntity<ResponseMessage<ReservationResponse>> updateByDesigner(
//            @PathVariable UUID id,
//            @RequestBody ReservationUpdateRequest request
//    ) {
//        UUID designerId = CurrentUserUtil.currentUserId();
//        ReservationResponse response = reservationService.updateByDesigner(designerId, id, request);
//        return ResponseEntity.ok(ResponseMessage.success("Designer reservation updated.", response));
//    }
//
//    @PreAuthorize("hasRole('DESIGNER')")
//    @Operation(summary = "Designer o'zi ishlaydigan reservationni bekor qilishi")
//    @PostMapping("/reservations/{id}/cancel")
//    public ResponseEntity<ResponseMessage<ReservationResponse>> cancelByDesigner(
//            @PathVariable UUID id
//    ) {
//        UUID designerId = CurrentUserUtil.currentUserId();
//        ReservationResponse response = reservationService.cancelByDesigner(designerId, id);
//        return ResponseEntity.ok(ResponseMessage.success("Designer reservation canceled.", response));
//    }

    @PreAuthorize("hasRole('DESIGNER')")
    @Operation(summary = "현재 로그인한 디자이너 자신의 예약 목록 (필터 포함)")
    @GetMapping("/reservations/list")
    public ResponseEntity<ResponseMessage<List<ReservationResponse>>> listReservations(

            @RequestParam(required = false)
            ReservationStatus status,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        // 🔹 항상 현재 로그인한 디자이너 기준
        UUID designerId = CurrentUserUtil.currentUserId();

        List<ReservationResponse> list = reservationService.listForDesignerFiltered(
                designerId,
                status,
                fromDate,
                toDate
        );

        return ResponseEntity.ok(
                ResponseMessage.success("Designer reservations fetched.", list)
        );
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
