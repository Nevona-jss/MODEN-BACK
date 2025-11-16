package com.moden.modenapi.modules.payment.controller;

import com.moden.modenapi.common.enums.PaymentStatus;
import com.moden.modenapi.common.enums.ServiceType;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.payment.dto.DesignerTipSummaryRes;
import com.moden.modenapi.modules.payment.dto.PaymentCreateReq;
import com.moden.modenapi.modules.payment.dto.PaymentRes;
import com.moden.modenapi.modules.payment.dto.TodaySalesSummaryRes;
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

@Tag(name = "HAIR STUDIO-PAYMENT")
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
    @PostMapping("/pay")
    public ResponseEntity<ResponseMessage<PaymentRes>> pay(
            @RequestBody PaymentCreateReq req
    ) {
        PaymentRes res = paymentService.confirmPayment(req);
        return ResponseEntity.ok(
                ResponseMessage.success("결제가 완료되었습니다.", res)
        );
    }

    // ============================
    // [1] 내 스튜디오 결제 목록 조회
    // ============================
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    @Operation(summary = "내 스튜디오 결제 목록 조회 (필터: 날짜, 서비스 타입, 디자이너, 상태)")
    @GetMapping("/filter")
    public ResponseEntity<ResponseMessage<List<PaymentRes>>> listPayments(
            @RequestParam(required = false) UUID designerId,
            @RequestParam(required = false) ServiceType serviceType,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        // 🔹 여기서 스튜디오 ID는 항상 현재 로그인한 계정에서 가져옴
        UUID studioId = CurrentUserUtil.currentUserId();

        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to = (toDate != null) ? toDate.plusDays(1).atStartOfDay() : null;

        List<PaymentRes> list = paymentService.searchPaymentsForList(
                studioId,        // studioId는 내부에서만 사용, 파라미터로 안 받음
                designerId,
                serviceType,
                status,
                from,
                to
        );

        return ResponseEntity.ok(
                ResponseMessage.success("내 스튜디오 결제 목록 조회가 완료되었습니다.", list)
        );
    }

    // ============================
    // [2] 내 스튜디오 기준 디자이너별 팁 합계 조회
    //  - designerId 안 받음 (전체 디자이너 요약)
    // ============================
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    @Operation(summary = "내 스튜디오 기준 디자이너별 팁 합계 조회")
    @GetMapping("/list/tip-summary")
    public ResponseEntity<ResponseMessage<List<DesignerTipSummaryRes>>> tipSummaryForStudio(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        UUID studioId = CurrentUserUtil.currentUserId();   // 🔹 로그인된 스튜디오

        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to = (toDate != null) ? toDate.plusDays(1).atStartOfDay() : null;

        // service 층에서 studioId 기준 + (필요하면 PAID만) 디자이너별 tip 합계 계산
        List<DesignerTipSummaryRes> list = paymentService.studioDesignerTipSummary(
                studioId,
                null,   // designerId 필터 없음 (전체)
                from,
                to
        );

        return ResponseEntity.ok(
                ResponseMessage.success("디자이너별 팁 합계 조회가 완료되었습니다.", list)
        );
    }

    // ============================
    // [3] 오늘 총 매출 / 결제 건수 / 평균 단가 (내 스튜디오 기준)
    // ============================
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    @Operation(summary = "오늘 총 매출 / 결제 건수 / 평균 결제 단가 조회 (내 스튜디오 기준)")
    @GetMapping("/stats/today")
    public ResponseEntity<ResponseMessage<TodaySalesSummaryRes>> getTodayStatsForMyStudio() {

        UUID studioId = CurrentUserUtil.currentUserId();   // 🔹 로그인된 스튜디오
        TodaySalesSummaryRes summary = paymentService.getTodaySummary(studioId);

        return ResponseEntity.ok(
                ResponseMessage.success("오늘 매출 요약 조회가 완료되었습니다.", summary)
        );
    }

    // ============================
    // [4] PAID(결제 완료) 상태 결제 목록 조회
    //  - studioId, designerId 둘 다 안 받는다
    //  - 현재 스튜디오 기준 + PAID 고정
    // ============================
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    @Operation(summary = "내 스튜디오 PAID(결제 완료) 상태 결제 목록 조회")
    @GetMapping("/list/paid")
    public ResponseEntity<ResponseMessage<List<PaymentRes>>> listPaidPayments(
            @RequestParam(required = false) ServiceType serviceType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        UUID studioId = CurrentUserUtil.currentUserId();   // 🔹 로그인된 스튜디오

        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to = (toDate != null) ? toDate.plusDays(1).atStartOfDay() : null;

        List<PaymentRes> list = paymentService.searchPaymentsForList(
                studioId,
                null,                 // designerId 필터 없음
                serviceType,
                PaymentStatus.PAID,   // 🔴 항상 PAID
                from,
                to
        );

        return ResponseEntity.ok(
                ResponseMessage.success("PAID 상태 결제 목록 조회가 완료되었습니다.", list)
        );
    }
}
