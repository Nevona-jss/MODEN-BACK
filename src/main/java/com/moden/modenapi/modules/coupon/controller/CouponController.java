package com.moden.modenapi.modules.coupon.controller;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.coupon.dto.CouponCreateRequest;
import com.moden.modenapi.modules.coupon.dto.CouponResponse;
import com.moden.modenapi.modules.coupon.dto.CouponUpdateRequest;
import com.moden.modenapi.modules.coupon.dto.CustomerCouponRes;
import com.moden.modenapi.modules.coupon.service.CouponService;
import com.moden.modenapi.modules.coupon.service.CustomerCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "HAIR STUDIO COUPON ")
@RestController
@RequestMapping("/api/studios/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CustomerCouponService customerCouponService;

    // ----------------------------------------------------------------------
    // CREATE (policy)
    // ----------------------------------------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Create coupon")
    @PostMapping("/create")
    public ResponseEntity<ResponseMessage<CouponResponse>> create(
            @Valid @RequestBody CouponCreateRequest req
    ) {
        var created = couponService.create(req);
        return ResponseEntity.ok(
                ResponseMessage.success("Coupon successfully created.", created)
        );
    }

    // ----------------------------------------------------------------------
    // UPDATE (PATCH, policy)
    // ----------------------------------------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Coupon update")
    @PatchMapping("/update/{id}")
    public ResponseEntity<ResponseMessage<CouponResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CouponUpdateRequest req
    ) {
        CouponResponse updated = couponService.update(id, req);
        return ResponseEntity.ok(
                ResponseMessage.success("Coupon updated successfully.", updated)
        );
    }

    // ----------------------------------------------------------------------
    // GET ONE (policy)
    // ----------------------------------------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @GetMapping("/get/{id}")
    @Operation(summary = "Get coupon detail by ID")
    public ResponseEntity<ResponseMessage<CouponResponse>> get(@PathVariable UUID id) {
        var res = couponService.get(id);
        return ResponseEntity.ok(ResponseMessage.success("Coupon fetched.", res));
    }

    // ----------------------------------------------------------------------
    // LIST (policy) - bitta /list, ichida status bo‘yicha filter
    // ----------------------------------------------------------------------
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(
            summary = "Studio coupon policy list (with optional status filter)",
            description = """
                    현재 로그인한 스튜디오 기준 쿠폰 정책 목록을 조회합니다.
                    - status 파라미터가 없으면 전체
                    - status 파라미터가 있으면 해당 상태의 쿠폰만 조회
                    """
    )
    public ResponseEntity<ResponseMessage<List<CouponResponse>>> listByStudio(
            @RequestParam(required = false) CouponStatus status
    ) {
        UUID currentUserId = CurrentUserUtil.currentUserId();  // USER ID

        List<CouponResponse> list;
        if (status == null) {
            // 🔹 status berilmasa – barcha policy
            list = couponService.listByStudioForCurrentUser(currentUserId);
        } else {
            // 🔹 status berilgan bo‘lsa – status bo‘yicha filter
            list = couponService.listByStudioAndStatusForCurrentUser(currentUserId, status);
        }

        return ResponseEntity.ok(
                ResponseMessage.success("Studio coupons fetched.", list)
        );
    }

    // ----------------------------------------------------------------------
    // STUDIO: 특정 userId(customer)의 쿠폰 리스트 보기
    //  (이건 policy가 아니라 실제 발급된 customer_coupon 기준이므로 별도 유지)
    // ----------------------------------------------------------------------
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    @Operation(
            summary = "List coupons for a customer (by userId)",
            description = "Studio userId (customer) kiritib, shu studioning ushbu customerga bergan barcha kuponlarini ko'radi."
    )
    @GetMapping("/customer/{userId}")
    public ResponseEntity<ResponseMessage<List<CustomerCouponRes>>> listCustomerCoupons(
            @PathVariable("userId") UUID customerUserId
    ) {
        var list = customerCouponService.listCouponsForCustomerUser(customerUserId);
        return ResponseEntity.ok(
                ResponseMessage.success("Customer coupons for this studio", list)
        );
    }
}
