package com.moden.modenapi.modules.customer.controller;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.common.enums.PointType;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.coupon.dto.CustomerCouponRes;
import com.moden.modenapi.modules.coupon.service.CustomerCouponService;
import com.moden.modenapi.modules.customer.dto.CustomerProfileUpdateReq;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.service.CustomerService;
import com.moden.modenapi.modules.point.dto.PointActiveSummaryRes;
import com.moden.modenapi.modules.point.dto.PointCustomerRes;
import com.moden.modenapi.modules.point.dto.PointSummaryRes;
import com.moden.modenapi.modules.point.service.PointService;
import com.moden.modenapi.modules.qa.dto.QACreateRequest;
import com.moden.modenapi.modules.qa.dto.QAResponse;
import com.moden.modenapi.modules.qa.service.QAService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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


    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "My point history with filters",
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


    // 🔹 Customer: Active point만 (usable balance)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "My active point balance")
    @GetMapping("/point/summary")
    public ResponseEntity<ResponseMessage<PointActiveSummaryRes>> mySummary() {
        UUID userId = CurrentUserUtil.currentUserId();
        var summary = pointService.getActiveSummary(userId);
        return ResponseEntity.ok(
                ResponseMessage.success("My active point summary", summary)
        );
    }


    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Mening kuponlarim", description = "customer_coupon jadvalidan, shu customerga tegishli kuponlar ro'yxati")
    @GetMapping("/coupon/list")
    public ResponseEntity<ResponseMessage<List<CustomerCouponRes>>> getMyCoupons(
            @RequestParam(name = "status", required = false) CouponStatus status
    ) {
        // 현재 로그인된 USER ID
        UUID currentUserId = CurrentUserUtil.currentUserId();

        // USER → CUSTOMER mapping 후, o'sha customerni kuponlarini 가져오기
        var list = customerCouponService.getCouponsForCurrentCustomerUser(currentUserId, status);

        return ResponseEntity.ok(
                ResponseMessage.success("Customer coupons fetched.", list)
        );
    }


    @PreAuthorize("hasAnyRole('CUSTOMER')")
    @PostMapping("/coupon/{customerCouponId}/use")
    public ResponseEntity<ResponseMessage<Void>> use(@PathVariable UUID customerCouponId) {
        UUID customerId = CurrentUserUtil.currentUserId();
        customerCouponService.useCustomerCoupon(customerCouponId, customerId);
        return ResponseEntity.ok(ResponseMessage.success("Used.", null));
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
    @Operation(summary = "Get my inquiry detail (customer)")
    @GetMapping("/qa/my/{qaId}")
    public ResponseEntity<ResponseMessage<QAResponse>> myDetail(
            @PathVariable UUID qaId
    ) {
        UUID userId = CurrentUserUtil.currentUserId();
        var res = qaService.getForCustomer(userId, qaId);
        return ResponseEntity.ok(ResponseMessage.success("Inquiry detail", res));
    }

}
