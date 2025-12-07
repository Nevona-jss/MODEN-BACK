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
    @PreAuthorize("hasRole('HAIR_STUDIO') or hasRole('DESIGNER')")
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
    @PatchMapping("/pay/{paymentId}")
    public ResponseEntity<ResponseMessage<PaymentRes>> pay(
            @PathVariable UUID paymentId,
            @RequestBody PaymentCreateReq req
    ) {
        PaymentRes res = paymentService.confirmPayment(paymentId, req);
        return ResponseEntity.ok(
                ResponseMessage.success("결제가 완료되었습니다.", res)
        );
    }


    @PreAuthorize("hasRole('HAIR_STUDIO')")
    @Operation(summary = "내 스튜디오 결제 목록 조회 (요약, pagination)")
    @GetMapping("/list/tip")
    public ResponseEntity<ResponseMessage<PaymentListPageRes>> getStudioPaymentList(
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

        PaymentListPageRes list = paymentService.getStudioPaymentList(
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

    // 스튜디오 기준 결제 목록 조회 (요약 + pagination)
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    @Operation(
            summary = "Studio 결제 목록 조회 (요약, pagination)",
            description = """
            현재 로그인한 헤어 스튜디오 계정 기준으로 결제 목록을 조회합니다.
            - designerId : 특정 디자이너 결제만 필터링 (옵션)
            - serviceName : 시술명 키워드 contains 필터 (옵션, in-memory)
            - status : 결제 상태 (PENDING / PAID 등, 옵션)
            - fromDate / toDate : 예약일 기준 범위 (YYYY-MM-DD, 옵션)
            - page / size : 페이징 파라미터
            """
    )
    @GetMapping("/studio/list")
    public ResponseEntity<ResponseMessage<PaymentListPageRes>> getStudioPaymentListForStudio(
            @RequestParam(required = false) UUID designerId,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        // 🔹 현재 로그인한 스튜디오 ID
        UUID studioId = CurrentUserUtil.currentUserId();

        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to   = (toDate != null)   ? toDate.plusDays(1).atStartOfDay() : null;

        PaymentListPageRes list = paymentService.getStudioPaymentList(
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


    @PreAuthorize("hasRole('HAIR_STUDIO') or hasRole('DESIGNER')")
    @Operation(summary = "Designer 결제 목록 조회 (디자이너 기준, 요약, pagination)")
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
        UUID designerId = CurrentUserUtil.currentUserId();

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

    @PreAuthorize("hasRole('HAIR_STUDIO') or hasRole('DESIGNER')")
    @Operation(
            summary = "오늘 총 매출 / 결제 건수 / 평균 결제 단가 조회 (내 스튜디오 기준)",
            description = """
        TodaySalesSummaryRes 필드 매핑:
        - summary.totalSales → 오늘 총 매출
        - summary.paymentCount → 오늘 결제 건수
        - summary.averageAmount → 평균 결제 단가
        """
    )
    @GetMapping("/stats/today")
    public ResponseEntity<ResponseMessage<TodaySalesSummaryRes>> getTodayStatsForMyStudio() {

        UUID userId = CurrentUserUtil.currentUserId();   // 스튜디오 or 디자이너 ID

        TodaySalesSummaryRes summary = paymentService.getTodaySummaryForCurrentUser(userId);

        return ResponseEntity.ok(
                ResponseMessage.success("오늘 매출 요약 조회가 완료되었습니다.", summary)
        );
    }


}
