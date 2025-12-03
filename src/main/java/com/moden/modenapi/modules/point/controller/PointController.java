package com.moden.modenapi.modules.point.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.customer.service.CustomerService;
import com.moden.modenapi.modules.point.dto.*;
import com.moden.modenapi.modules.point.service.PointService;
import com.moden.modenapi.modules.point.service.StudioPointPolicyService;
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

@Tag(name = "POINT", description = "Studio side point management API")
@RestController
@RequestMapping("/api/studios/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;
    private final StudioPointPolicyService studioPointPolicyService;
    private final CustomerService customerService;

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Get point policy for current studio")
    @GetMapping("/policy/get")
    public ResponseEntity<ResponseMessage<StudioPointPolicyRes>> getMyStudioPolicy() {
        var studioId = CurrentUserUtil.currentUserId();
        var res = studioPointPolicyService.getPolicyForStudio(studioId);
        return ResponseEntity.ok(ResponseMessage.success("Point policy loaded", res));
    }

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Create or update point policy for current studio")
    @PatchMapping ("/policy/update")
    public ResponseEntity<ResponseMessage<StudioPointPolicyRes>> updateMyStudioPolicy(
            @Valid @RequestBody StudioPointPolicyReq req
    ) {
        var studioId = CurrentUserUtil.currentUserId();
        var res = studioPointPolicyService.upsertPolicy(studioId, req);
        return ResponseEntity.ok(ResponseMessage.success("Point policy updated", res));
    }

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Get point by ID")
    @GetMapping("/get/{pointId}")
    public ResponseEntity<ResponseMessage<PointRes>> get(@PathVariable UUID pointId) {
        PointRes res = pointService.getPoint(pointId);
        return ResponseEntity.ok(ResponseMessage.success(res));
    }

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Soft delete a point record")
    @DeleteMapping("/delete/{pointId}")
    public ResponseEntity<Void> delete(@PathVariable UUID pointId) {
        pointService.softDelete(pointId);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Grant points manually to user (studio/admin)")
    @PostMapping("/create/manual")
    public ResponseEntity<ResponseMessage<PointRes>> grantManual(
            @Valid @RequestBody PointManualGrantReq req
    ) {
        PointRes res = pointService.grantManual(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Manual point granted", res));
    }

    // 🔹 STUDIO: berilgan userId bo'yicha, shu studiyoga tegishli customer point history
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(
            summary = "List points for a customer (by userId)",
            description = "Studio userId kiritganda, agar shu customer studiyoga tegishli bo'lsa, uning barcha pointlari qaytadi."
    )
    @GetMapping("/customer/list/{userId}")
    public ResponseEntity<ResponseMessage<List<PointRes>>> listCustomerPoints(
            @PathVariable UUID userId
    ) {
        // 1) bu user shu studiyoga tegishlimi? (bo'lmasa 403)
        customerService.ensureCustomerOfCurrentStudio(userId);

        // 2) user bo'yicha pointlar
        var list = pointService.listByUser(userId);
        return ResponseEntity.ok(
                ResponseMessage.success("Customer point history", list)
        );
    }

    // 🔹 STUDIO: berilgan userId bo'yicha point summary (earned/used/balance)
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(
            summary = "Point summary for a customer (by userId)",
            description = "Studio userId kiritganda, shu customer uchun earned/used/balance qaytadi."
    )
    @GetMapping("/customer/{userId}/summary")
    public ResponseEntity<ResponseMessage<PointSummaryRes>> customerPointSummary(
            @PathVariable UUID userId
    ) {
        customerService.ensureCustomerOfCurrentStudio(userId);

        PointSummaryRes res = pointService.getSummary(userId);
        return ResponseEntity.ok(
                ResponseMessage.success("Customer point summary", res)
        );
    }

}
