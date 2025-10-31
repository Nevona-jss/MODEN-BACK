package com.moden.modenapi.modules.coupon.controller;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.coupon.dto.CouponCreateReq;
import com.moden.modenapi.modules.coupon.dto.CouponRes;
import com.moden.modenapi.modules.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Coupon", description = "Coupon management API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "Issue new coupon (쿠폰 발급)")
    @PostMapping("/studios/coupon/create")
    public ResponseEntity<ResponseMessage<CouponRes>> create(@RequestBody CouponCreateReq req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Coupon issued successfully", couponService.create(req)));
    }

    @Operation(summary = "Mark coupon as used (사용완료)")
    @PostMapping("/customers/{couponId}/use")
    public ResponseEntity<ResponseMessage<CouponRes>> markUsed(@PathVariable UUID couponId) {
        return ResponseEntity.ok(ResponseMessage.success("Coupon marked as used", couponService.markAsUsed(couponId)));
    }

    @Operation(summary = "List user coupons (고객 쿠폰 목록)")
    @GetMapping("/{userId}")
    public ResponseEntity<ResponseMessage<List<CouponRes>>> listByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ResponseMessage.success("Coupons fetched", couponService.listByUser(userId)));
    }


    @Operation(summary = "List user coupons by status", description = "사용 가능 / 사용 완료 / 만료됨 필터로 조회")
    @GetMapping("/{userId}/filter")
    public ResponseEntity<ResponseMessage<List<CouponRes>>> listByUserAndStatus(
            @PathVariable UUID userId,
            @RequestParam(required = false) CouponStatus status
    ) {
        List<CouponRes> list = couponService.listByUserAndStatus(userId, status);
        return ResponseEntity.ok(ResponseMessage.success("Coupons fetched successfully", list));
    }


}
