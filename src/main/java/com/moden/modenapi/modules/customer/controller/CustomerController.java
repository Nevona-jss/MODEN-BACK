package com.moden.modenapi.modules.customer.controller;

import com.moden.modenapi.common.dto.FilterParams;
import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.common.enums.PointType;
import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.consultation.dto.ConsultationFilter;
import com.moden.modenapi.modules.consultation.dto.ConsultationRes;
import com.moden.modenapi.modules.consultation.dto.CustomerMemoUpdateReq;
import com.moden.modenapi.modules.consultation.service.ConsultationService;
import com.moden.modenapi.modules.coupon.dto.CustomerCouponRes;
import com.moden.modenapi.modules.coupon.service.CustomerCouponService;
import com.moden.modenapi.modules.customer.dto.CustomerProfileUpdateReq;
import com.moden.modenapi.modules.customer.dto.MySummaryRes;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.service.CustomerService;
import com.moden.modenapi.modules.point.dto.PointActiveSummaryRes;
import com.moden.modenapi.modules.point.dto.PointCustomerRes;
import com.moden.modenapi.modules.point.service.PointService;
import com.moden.modenapi.modules.qa.dto.QACreateRequest;
import com.moden.modenapi.modules.qa.dto.QAResponse;
import com.moden.modenapi.modules.qa.service.QAService;
import com.moden.modenapi.modules.reservation.dto.ReservationCreateRequest;
import com.moden.modenapi.modules.reservation.dto.ReservationResponse;
import com.moden.modenapi.modules.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(
        name = "CUSTOMER")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;
    private final CustomerCouponService customerCouponService;
    private final PointService pointService;
    private final QAService qaService;
    private final ReservationService reservationService;
    private final ConsultationService consultationService;


    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Mening reservationlarim (filter bilan)",
            description = "status / fromDate / toDate bo‘yicha filter. Param bermasang – hammasi chiqadi."
    )
    @GetMapping("/reservation/list")
    public ResponseEntity<ResponseMessage<List<ReservationResponse>>> myReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        UUID currentUserId = CurrentUserUtil.currentUserId();
        List<ReservationResponse> list = reservationService.listForCustomerFiltered(
                currentUserId,
                status,
                fromDate,
                toDate
        );
        return ResponseEntity.ok(ResponseMessage.success("My reservations fetched.", list));
    }


    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "My point history (filter bilan)",
            description = "type=EARNED/USED, period=TODAY/WEEK/MONTH/ALL"
    )
    @GetMapping("/points/list")
    public ResponseEntity<ResponseMessage<List<PointCustomerRes>>> myPoints(
            @RequestParam(required = false) PointType type,
            @RequestParam(required = false, defaultValue = "ALL") String period
    ) {
        UUID userId = CurrentUserUtil.currentUserId();
        var list = pointService.listForCustomer(userId, type, period);
        return ResponseEntity.ok(ResponseMessage.success("My point history", list));
    }


    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "My summary: available coupons + active point")
    @GetMapping("/summary")
    public ResponseEntity<ResponseMessage<MySummaryRes>> myOverallSummary() {
        UUID currentUserId = CurrentUserUtil.currentUserId();

        // 1) 사용 가능한 쿠폰 개수
        byte couponCount = customerCouponService
                .countAvailableCouponsForCurrentCustomerUser(currentUserId);

        // 2) 활성 포인트 합계
        PointActiveSummaryRes pointSummary = pointService.getActiveSummary(currentUserId);

        // 3) 평탄화된 DTO 로 감싸기
        MySummaryRes res = new MySummaryRes(
                couponCount,
                pointSummary.activePoint()   // ← BigDecimal
        );

        return ResponseEntity.ok(
                ResponseMessage.success("My coupon & point summary", res)
        );
    }




    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Mening kuponlarim (filter bilan)",
            description = "status=AVAILABLE/USED/EXPIRED va h.k., period=TODAY/WEEK/MONTH/ALL, serviceNames=multi select bo‘yicha filter"
    )
    @GetMapping("/coupon/list")
    public ResponseEntity<ResponseMessage<List<CustomerCouponRes>>> getMyCoupons(
            @RequestParam(name = "status", required = false) CouponStatus status,
            @RequestParam(name = "period", required = false, defaultValue = "ALL") String period,
            @RequestParam(name = "serviceNames", required = false) List<String> serviceNames
    ) {
        UUID currentUserId = CurrentUserUtil.currentUserId();
        var list = customerCouponService.getCouponsForCurrentCustomerUser(
                currentUserId,
                status,
                period,
                serviceNames
        );
        return ResponseEntity.ok(
                ResponseMessage.success("Customer coupons fetched.", list)
        );
    }

    @Operation(summary = "Customer: update my profile")
    @PatchMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ResponseMessage<CustomerDetail>> updateMyProfile(@Valid @RequestBody CustomerProfileUpdateReq req) {
        var out = service.updateOwnProfile(req);
        return ResponseEntity.ok(ResponseMessage.success("Profile updated", out));
    }


    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create new inquiry (customer)")
    @PostMapping("qa/create")
    public ResponseEntity<ResponseMessage<QAResponse>> create(
            @Valid @RequestBody QACreateRequest req
    ) {
        UUID userId = CurrentUserUtil.currentUserId();
        QAResponse res = qaService.createByCustomer(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Inquiry created", res));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "List my inquiries (customer)")
    @GetMapping("/qa/my")
    public ResponseEntity<ResponseMessage<List<QAResponse>>> myList() {
        UUID userId = CurrentUserUtil.currentUserId();
        var list = qaService.listForCustomer(userId);
        return ResponseEntity.ok(ResponseMessage.success("My inquiries", list));
    }


    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/consultation/list")
    public ResponseEntity<ResponseMessage<List<ConsultationRes>>> listConsultation(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> serviceNames,
            @RequestParam(required = false) String period
    ) {
        FilterParams filter = new FilterParams(
                keyword,
                serviceNames,
                period
        );

        List<ConsultationRes> list = consultationService.listForCustomerFiltered(filter);
        return ResponseEntity.ok(ResponseMessage.success("Consultation list fetched", list));
    }







    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "내 상담에 고객 메모 작성/수정",
            description = """
                    고객 본인이 소유한 상담에 대해 customerMemo(내 메모)를 작성/수정합니다.
                    - body에 customerMemo 만 전달하면 됩니다.
                    - 해당 상담이 내 예약이 아니면 403(FORBIDDEN)이 반환됩니다.
                    """)
    @PatchMapping("/consultation/create/{consultationId}/memo")
    public ResponseEntity<ResponseMessage<ConsultationRes>> updateCustomerMemo(
            @PathVariable UUID consultationId,
            @RequestBody CustomerMemoUpdateReq request
    ) {
        UUID customerId = CurrentUserUtil.currentUserId();
        ConsultationRes res = consultationService.updateCustomerMemo(customerId, consultationId, request);
        return ResponseEntity.ok(ResponseMessage.success("고객 메모가 저장되었습니다.", res));
    }

}
