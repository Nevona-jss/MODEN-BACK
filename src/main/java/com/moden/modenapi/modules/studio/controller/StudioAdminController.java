package com.moden.modenapi.modules.studio.controller;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.customer.dto.CustomerProfileUpdateReq;
import com.moden.modenapi.modules.customer.dto.CustomerResponse;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.service.CustomerService;
import com.moden.modenapi.modules.designer.dto.DesignerCreateDto;
import com.moden.modenapi.modules.designer.dto.DesignerResponse;
import com.moden.modenapi.modules.designer.service.DesignerService;
import com.moden.modenapi.modules.reservation.dto.ReservationResponse;
import com.moden.modenapi.modules.reservation.service.ReservationService;
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

    // ----------------------------------------------------------------------
    // 🔹 STUDIO/ADMIN: Create designer
    // ----------------------------------------------------------------------
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


    // ----------------------------------------------------------------------
    // 🔹 Studio: 고객 리스트
    // ----------------------------------------------------------------------
    @Operation(summary = "Studio: list my customers")
    @GetMapping("/customers/list")
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    public ResponseEntity<ResponseMessage<List<CustomerResponse>>> listStudioCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        var list = customerService.listStudioCustomers(keyword, fromDate, toDate);
        return ResponseEntity.ok(
                ResponseMessage.success("Designer list for current studio", list)
        );    }


    @PreAuthorize("hasRole('HAIR_STUDIO')")
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

    // ----------------------------------------------------------------------
    // 🔹 특정 디자이너 조회
    // ----------------------------------------------------------------------
    @Operation(
            summary = "Get designer by ID",
            description = "Returns a single designer profile (requires admin/studio permission)."
    )
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
    @GetMapping("/list")
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
    @PreAuthorize("hasRole('HAIR_STUDIO')")
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
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    public ResponseEntity<ResponseMessage<Void>> deleteCustomer(
            @PathVariable UUID customerUserId
    ) {
        customerService.deleteCustomerAsStudio(customerUserId);
        return ResponseEntity.ok(
                ResponseMessage.success("Deleted", null)
        );
    }
}
