package com.moden.modenapi.modules.customer.controller;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.coupon.dto.CustomerCouponRes;
import com.moden.modenapi.modules.coupon.service.CustomerCouponService;
import com.moden.modenapi.modules.customer.dto.CustomerProfileUpdateReq;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(
        name = "Customer APIs")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;
    private final CustomerCouponService customerCouponService;

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

}
