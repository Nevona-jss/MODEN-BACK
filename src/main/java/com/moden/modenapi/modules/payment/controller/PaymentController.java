package com.moden.modenapi.modules.payment.controller;

import com.moden.modenapi.common.enums.PaymentStatus;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.payment.dto.*;
import com.moden.modenapi.modules.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "PAYMENT")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 1) 예약 기준 결제 상세 조회 (UNPAID / PAID 상태 상관 없이)
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "예약 기준 결제 상세 조회")
    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<ResponseMessage<PaymentRes>> getByReservation(
            @PathVariable UUID reservationId
    ) {
        PaymentRes res = paymentService.getByReservation(reservationId);
        return ResponseEntity.ok(
                ResponseMessage.success("결제 정보 조회가 완료되었습니다.", res)
        );
    }

    // 2) 결제 확정 (포인트 + 쿠폰 + 제품 합계 적용)
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "오프라인 결제 확정 (포인트/쿠폰 적용)")
    @PatchMapping("/pay")
    public ResponseEntity<ResponseMessage<PaymentRes>> pay(
            @RequestBody PaymentCreateReq req
    ) {
        PaymentRes res = paymentService.confirmPayment(req);
        return ResponseEntity.ok(
                ResponseMessage.success("결제가 완료되었습니다.", res)
        );
    }

    @PreAuthorize("hasRole('HAIR_STUDIO')")
    @Operation(summary = "내 스튜디오 결제 목록 조회 (요약, pagination)")
    @GetMapping("/list/tip")
    public ResponseEntity<ResponseMessage<List<PaymentListItemRes>>> getStudioPaymentList(
            @RequestParam(required = false) UUID designerId,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID studioId = CurrentUserUtil.currentUserId();   // 🔹 현재 로그인 스튜디오

        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to   = (toDate != null)   ? toDate.plusDays(1).atStartOfDay() : null;

        List<PaymentListItemRes> list = paymentService.getStudioPaymentList(
                studioId,
                designerId,
                serviceName,
                from,
                to,
                status,
                page,
                size
        );

        return ResponseEntity.ok(
                ResponseMessage.success("스튜디오 결제 목록(요약) 조회가 완료되었습니다.", list)
        );
    }

    @PreAuthorize("hasRole('DESIGNER')")
    @Operation(summary = "내 결제 목록 조회 (디자이너 기준, 요약, pagination)")
    @GetMapping("/list")
    public ResponseEntity<ResponseMessage<List<PaymentListItemRes>>> getDesignerPaymentList(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID designerId = CurrentUserUtil.currentUserId();   // 🔹 현재 로그인 디자이너

        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to   = (toDate != null)   ? toDate.plusDays(1).atStartOfDay() : null;

        List<PaymentListItemRes> list = paymentService.getDesignerPaymentList(
                designerId,
                serviceName,
                from,
                to,
                status,
                page,
                size
        );

        return ResponseEntity.ok(
                ResponseMessage.success("디자이너 결제 목록(요약) 조회가 완료되었습니다.", list)
        );
    }

    // [3-1] 오늘 총 매출 / 결제 건수 / 평균 단가 (내 스튜디오 기준)
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(
            summary = "오늘 총 매출 / 결제 건수 / 평균 결제 단가 조회 (내 스튜디오 기준)",
            description = """
        TodaySalesSummaryRes 필드 매핑:
        - summary.totalSales → 오늘 총 매출
        - summary.paymentCount → 오늘 결제 건수
        - summary.averageAmount → 평균 결제 단가
        """
    )    @GetMapping("/stats/today")
    public ResponseEntity<ResponseMessage<TodaySalesSummaryRes>> getTodayStatsForMyStudio() {

        UUID studioId = CurrentUserUtil.currentUserId();
        TodaySalesSummaryRes summary = paymentService.getTodaySummary(studioId);

        return ResponseEntity.ok(
                ResponseMessage.success("오늘 매출 요약 조회가 완료되었습니다.", summary)
        );
    }

}
