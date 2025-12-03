package com.moden.modenapi.modules.studio.controller;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.common.enums.PointType;
import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.consultation.dto.ConsultationRes;
import com.moden.modenapi.modules.consultation.service.ConsultationService;
import com.moden.modenapi.modules.coupon.dto.CouponResponse;
import com.moden.modenapi.modules.coupon.service.CouponService;
import com.moden.modenapi.modules.customer.dto.CustomerListPageRes;
import com.moden.modenapi.modules.customer.dto.CustomerProfileUpdateReq;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.service.CustomerService;
import com.moden.modenapi.modules.designer.dto.DesignerCreateDto;
import com.moden.modenapi.modules.designer.dto.DesignerResponse;
import com.moden.modenapi.modules.designer.dto.DesignerUpdateReq;
import com.moden.modenapi.modules.designer.service.DesignerService;
import com.moden.modenapi.modules.point.dto.PointCustomerRes;
import com.moden.modenapi.modules.point.service.PointService;
import com.moden.modenapi.modules.reservation.dto.ReservationResponse;
import com.moden.modenapi.modules.reservation.service.ReservationService;
import com.moden.modenapi.modules.studio.dto.StudioBirthdayCouponRequest;
import com.moden.modenapi.modules.studio.dto.StudioPrivacyPolicyRequest;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.dto.StudioUpdateReq;
import com.moden.modenapi.modules.studio.service.HairStudioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "HAIR STUDIO")
@RestController
@RequestMapping("/api/studios")
@RequiredArgsConstructor
public class StudioAdminController {

    private final HairStudioService studioService;
    private final DesignerService designerService;
    private final CustomerService customerService;
    private final ReservationService reservationService;
    private final ConsultationService consultationService;
    private final CouponService couponService;
    private final PointService pointService;


    /**
     * 1) Hozir login bo'lgan studio owner uchun
     * tug'ilgan kun kupon setting update
     */
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @PatchMapping("/update/birthday-coupon")
    public ResponseMessage<String> updateBirthdayCoupon(
            @RequestBody StudioBirthdayCouponRequest req
    ) {
        UUID userId = CurrentUserUtil.currentUserId();

        studioService.updateBirthdayCouponSettings(userId, req);

        return ResponseMessage.success("Birthday coupon settings updated");
    }

    /**
     * 2) Hozir login bo'lgan studio owner uchun
     * 개인정보/보안 안내 HTML update
     */
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    @PatchMapping("/update/privacy-policy")
    public ResponseMessage<String> updatePrivacyPolicy(
            @RequestBody StudioPrivacyPolicyRequest req
    ) {
        UUID userId = CurrentUserUtil.currentUserId();  // 🔹 shu yerda ham

        studioService.updatePrivacyPolicyHtml(userId, req);

        return ResponseMessage.success("Privacy policy updated");
    }
    // ----------------------------------------------------------------------
    // 🔹 STUDIO: 자기 프로필 수정 (partial)
    // ----------------------------------------------------------------------
    @Operation(summary = "Studio: Update my profile (partial)")
    @PatchMapping(path = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    public ResponseEntity<ResponseMessage<StudioRes>> updateMyProfile(
            @RequestBody StudioUpdateReq req
    ) {
        // ✅ 공통 유틸 사용
        UUID userId = CurrentUserUtil.currentUserId();
        StudioRes res = studioService.updateSelf(userId, req);

        return ResponseEntity.ok(
                ResponseMessage.success("Studio updated", res)
        );
    }

    @PreAuthorize("hasRole('HAIR_STUDIO')")
    @PostMapping(
            value = "/designer/register",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResponseMessage<DesignerResponse>> createDesigner(
            HttpServletRequest request,
            @Valid @RequestBody DesignerCreateDto req
    ) {
        var created = designerService.createDesigner(request, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Designer created successfully", created));
    }

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @PatchMapping("/designers/update/{userId}")
    public ResponseEntity<ResponseMessage<DesignerResponse>> updateDesignerProfileByStudio(
            HttpServletRequest request,
            @PathVariable UUID userId,
            @RequestBody DesignerUpdateReq req
    ) {
        var updated = designerService.updateProfileByStudio(request, userId, req);
        return ResponseEntity.ok(ResponseMessage.success("Designer profile updated", updated));
    }


    // ----------------------------------------------------------------------
    // 🔹 Studio: 고객 리스트
    // ----------------------------------------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @GetMapping("customer/list")
    public ResponseEntity<ResponseMessage<CustomerListPageRes>> listStudioCustomers(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        var data = customerService.listStudioCustomers(keyword, fromDate, toDate, page, limit);
        return ResponseEntity.ok(
                ResponseMessage.success("Customer filtered list.", data)
        );
    }



    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(
            summary = "List designers for current studio (filter bilan)",
            description = """
                현재 스튜디오 기준 디자이너 목록 조회.
                - keyword   : 디자이너 이름 / 이메일 / 닉네임
                - onlyActive: true 일 때 삭제되지 않은(활성) 디자이너만
                """
    )
    @GetMapping("/designer/list")
    public ResponseEntity<ResponseMessage<List<DesignerResponse>>> listDesignersForCurrentStudio(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "false") boolean onlyActive
    ) {
        var list = designerService.listDesignersForCurrentStudio(keyword, onlyActive);
        return ResponseEntity.ok(
                ResponseMessage.success("Designer list for current studio", list)
        );
    }

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @GetMapping("/customer/{customerUserId}")
    public ResponseEntity<ResponseMessage<?>> getCustomerOfStudio(
            @PathVariable("customerUserId") UUID customerUserId
    ) {
        var res = customerService.getCustomerProfileForStudio(customerUserId);
        return ResponseEntity.ok(ResponseMessage.success("Customer loaded", res));
    }

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @GetMapping("/customer/consultation/list/{customerId}")
    public ResponseEntity<ResponseMessage<List<ConsultationRes>>> listConsultationsForCustomerInStudio(
            @PathVariable UUID customerId,
            @RequestParam(required = false) ConsultationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        var list = consultationService.listForStudioByCustomer(
                customerId,
                status,
                fromDate,
                toDate
        );
        return ResponseEntity.ok(ResponseMessage.success("OK", list));
    }


    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @GetMapping("/customer/coupon/list/{customerId}")
    public ResponseEntity<ResponseMessage<List<CouponResponse>>> listCouponsForCustomer(
            @PathVariable UUID customerId,
            @RequestParam(required = false) CouponStatus status
    ) {
        var list = couponService.listForCustomer(customerId, status);
        return ResponseEntity.ok(ResponseMessage.success("OK", list));
    }


    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @GetMapping("/customer/point/list/{customerId}")
    public ResponseEntity<ResponseMessage<List<PointCustomerRes>>> listPointsForCustomer(
            @PathVariable UUID customerId,
            @RequestParam(required = false) PointType type,
            @RequestParam(required = false, defaultValue = "ALL") String period
    ) {
        var list = pointService.listForCustomer(customerId, type, period);
        return ResponseEntity.ok(ResponseMessage.success("OK", list));
    }

    // ----------------------------------------------------------------------
    // 🔹 특정 디자이너 조회
    // ----------------------------------------------------------------------
    @Operation(
            summary = "Get designer by ID",
            description = "Returns a single designer profile (requires admin/studio permission)."
    )
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @GetMapping("/designer/{designerId}")
    public ResponseEntity<ResponseMessage<DesignerResponse>> getDesigner(
            @PathVariable UUID designerId
    ) {
        var data = designerService.getProfile(designerId);
        return ResponseEntity.ok(
                ResponseMessage.success("Designer fetched successfully", data)
        );
    }

    // ----------------------------------------------------------------------
    // 🔹 디자이너 삭제 (soft delete)
    // ----------------------------------------------------------------------
    @Operation(
            summary = "Delete designer (soft delete)",
            description = "Studio can delete its own designer. Marks deleted_at; portfolio untouched."
    )
    @PreAuthorize("hasRole('HAIR_STUDIO') or hasRole('ADMIN')")
    @DeleteMapping("/designer/delete/{id}")
    public ResponseEntity<ResponseMessage<Void>> deleteDesigner(
            HttpServletRequest request,
            @PathVariable("id") UUID designerId
    ) {
        designerService.deleteDesigner(request, designerId);
        return ResponseEntity.ok(
                ResponseMessage.success("Designer soft-deleted", null)
        );
    }

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Reservation list (filter + pagination)")
    @GetMapping("/reservation/list")
    public ResponseEntity<ResponseMessage<List<ReservationResponse>>> listDynamic(
            @RequestParam(required = false) UUID designerId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        var list = reservationService.searchDynamic(
                designerId,
                customerId,
                serviceId,
                status,
                fromDate,
                toDate,
                page,
                size
        );
        return ResponseEntity.ok(
                ResponseMessage.success("Reservation filtered list (paged).", list)
        );
    }


    // ----------------------------------------------------------------------
    // 🔹 Studio: 고객 정보 수정
    // ----------------------------------------------------------------------
    @Operation(summary = "Studio: update a customer in my studio")
    @PatchMapping("/customers/update/{customerUserId}")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    public ResponseEntity<ResponseMessage<CustomerDetail>> updateCustomer(
            @PathVariable UUID customerUserId,
            @Valid @RequestBody CustomerProfileUpdateReq req
    ) {
        var out = customerService.updateCustomerAsStudio(customerUserId, req);
        return ResponseEntity.ok(
                ResponseMessage.success("Updated", out)
        );
    }

    // ----------------------------------------------------------------------
    // 🔹 Studio: 고객 삭제
    // ----------------------------------------------------------------------
    @Operation(summary = "Studio: delete a customer in my studio")
    @DeleteMapping("/customer/delete/{customerUserId}")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    public ResponseEntity<ResponseMessage<Void>> deleteCustomer(
            @PathVariable UUID customerUserId
    ) {
        customerService.deleteCustomerAsStudio(customerUserId);
        return ResponseEntity.ok(
                ResponseMessage.success("Deleted", null)
        );
    }
}
