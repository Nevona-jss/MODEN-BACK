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

    // ----------------------------------------------------------------------
    // CREATE (policy)
    // ----------------------------------------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Create coupon")
    @PostMapping("/create")
    public ResponseEntity<ResponseMessage<CouponResponse>> create(@Valid @RequestBody CouponCreateRequest req) {
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
        // ✅ bu yerda DB dan kuponni yangilab, eng so‘nggi holatini qaytaramiz
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
    public ResponseEntity<ResponseMessage<CouponResponse>> get(@PathVariable UUID id) {
        var res = couponService.get(id);
        return ResponseEntity.ok(ResponseMessage.success("Coupon fetched.", res));
    }

    // ----------------------------------------------------------------------
    // LIST BY STUDIO (policy)
    // ----------------------------------------------------------------------
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Studio bo‘yicha barcha kupon policy-lari")
    public ResponseEntity<ResponseMessage<List<CouponResponse>>> listByStudio() {
        UUID currentUserId = CurrentUserUtil.currentUserId();  // USER ID
        var list = couponService.listByStudioForCurrentUser(currentUserId);  // ✅ userId 기반
        return ResponseEntity.ok(ResponseMessage.success("Studio coupons fetched.", list));
    }

    // ----------------------------------------------------------------------
// LIST BY STUDIO + STATUS (policy)
// ----------------------------------------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Studio kupon policy-lari (status bo‘yicha)")
    @GetMapping("/filter")
    public ResponseEntity<ResponseMessage<List<CouponResponse>>> listByStudioAndStatus(
            @RequestParam(required = false) CouponStatus status
    ) {
        UUID currentUserId = CurrentUserUtil.currentUserId();   // 🔹 USER ID
        var list = couponService.listByStudioAndStatusForCurrentUser(currentUserId, status); // 🔹 수정된 서비스 호출

        return ResponseEntity.ok(
                ResponseMessage.success("Studio coupons fetched.", list)
        );
    }

}
