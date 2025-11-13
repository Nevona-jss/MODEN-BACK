package com.moden.modenapi.modules.coupon.controller;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.coupon.dto.CouponCreateRequest;
import com.moden.modenapi.modules.coupon.dto.CouponResponse;
import com.moden.modenapi.modules.coupon.dto.CouponUpdateRequest;
import com.moden.modenapi.modules.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "HAIR STUDIO COUPON")
@RestController
@RequestMapping("/api/studios/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // ----------------------------------------------------------------------
    // CREATE
    // ----------------------------------------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Kupon yaratish")
    @PostMapping("/create")
    public ResponseEntity<ResponseMessage<Void>> create(@Valid @RequestBody CouponCreateRequest req) {
        couponService.create(req);
        return ResponseEntity.ok(
                ResponseMessage.<Void>builder()
                        .success(true)
                        .message("Coupon successfully created.")
                        .build()
        );
    }

    // ----------------------------------------------------------------------
    // UPDATE (PATCH)
    // ----------------------------------------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Kuponni qisman yangilash (PATCH)")
    @PatchMapping("/update/{id}")
    public ResponseEntity<ResponseMessage<CouponResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CouponUpdateRequest req
    ) {
        var updated = couponService.update(id, req);
        return ResponseEntity.ok(ResponseMessage.success("Coupon updated successfully.", updated));
    }

    // ----------------------------------------------------------------------
    // MARK AS USED
    // ----------------------------------------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Kuponni ishlatilgan deb belgilash")
    @PostMapping("/{id}/use")
    public ResponseEntity<ResponseMessage<CouponResponse>> markUsed(@PathVariable UUID id) {
        var res = couponService.markAsUsed(id);
        return ResponseEntity.ok(ResponseMessage.success("Coupon marked as used.", res));
    }

    // ----------------------------------------------------------------------
    // GET ONE
    // ----------------------------------------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER','CUSTOMER')")
    @Operation(summary = "Bitta kuponni olish")
    @GetMapping("/get/{id}")
    public ResponseEntity<ResponseMessage<CouponResponse>> get(@PathVariable UUID id) {
        var res = couponService.get(id);
        return ResponseEntity.ok(ResponseMessage.success("Coupon fetched.", res));
    }

    // ----------------------------------------------------------------------
    // LIST BY USER
    // ----------------------------------------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER','CUSTOMER')")
    @Operation(summary = "Foydalanuvchi kuponlari ro‘yxati")
    @GetMapping("/listByUser/{customerId}")
    public ResponseEntity<ResponseMessage<List<CouponResponse>>> listByUser(
            @PathVariable UUID customerId
    ) {
        var list = couponService.listByUser(customerId);
        return ResponseEntity.ok(ResponseMessage.success("Coupons fetched.", list));
    }

    // ----------------------------------------------------------------------
    // LIST BY USER + STATUS
    // ----------------------------------------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER','CUSTOMER')")
    @Operation(summary = "Foydalanuvchi kuponlari (status bo‘yicha)")
    @GetMapping("/listByUserAndStatus/{customerId}/filter")
    public ResponseEntity<ResponseMessage<List<CouponResponse>>> listByUserAndStatus(
            @PathVariable UUID customerId,
            @RequestParam(required = false) CouponStatus status
    ) {
        var list = couponService.listByUserAndStatus(customerId, status);
        return ResponseEntity.ok(ResponseMessage.success("Coupons fetched.", list));
    }
    

    // Studio bo‘yicha barcha kuponlar
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Studio bo‘yicha barcha kuponlar")
    @GetMapping("/list")
    public ResponseEntity<ResponseMessage<List<CouponResponse>>> listByStudio() {
        UUID currentUserId = CurrentUserUtil.currentUserId();
        var list = couponService.listByStudio(currentUserId);
        return ResponseEntity.ok(ResponseMessage.success("Studio coupons fetched.", list));
    }

    // Studio kuponlari (status bo‘yicha)
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Studio kuponlari (status bo‘yicha)")
    @GetMapping("/filter")
    public ResponseEntity<ResponseMessage<List<CouponResponse>>> listByStudioAndStatus(
            @RequestParam(required = false) CouponStatus status
    ) {
        UUID currentUserId = CurrentUserUtil.currentUserId();
        var list = couponService.listByStudioAndStatus(currentUserId, status);
        return ResponseEntity.ok(ResponseMessage.success("Studio coupons fetched.", list));
    }

    // Bugungi tug‘ilgan mijozlarga kupon berish
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Bugungi tug‘ilgan mijozlarga kupon berish (bitta studio uchun)")
    @PostMapping("/issue-birthday-today")
    public ResponseEntity<ResponseMessage<Void>> issueBirthdayToday() {
        UUID currentUserId = CurrentUserUtil.currentUserId();
        couponService.issueBirthdayCouponsForStudio(currentUserId);
        return ResponseEntity.ok(ResponseMessage.success("Birthday coupons issued (today).", null));
    }

}
