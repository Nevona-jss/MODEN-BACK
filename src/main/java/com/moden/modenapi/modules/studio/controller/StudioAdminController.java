package com.moden.modenapi.modules.studio.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.auth.service.AuthService;
import com.moden.modenapi.modules.customer.dto.CustomerProfileUpdateReq;
import com.moden.modenapi.modules.customer.dto.CustomerSignUpRequest;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.service.CustomerService;
import com.moden.modenapi.modules.designer.dto.DesignerCreateDto;
import com.moden.modenapi.modules.designer.dto.DesignerResponse;
import com.moden.modenapi.modules.designer.service.DesignerService;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.dto.StudioUpdateReq;
import com.moden.modenapi.modules.studio.service.HairStudioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "HAIR STUDIO")
@RestController
@RequestMapping("/api/studios")
@RequiredArgsConstructor
public class StudioAdminController {

    private final HairStudioService  studioService;
    private final DesignerService designerService;
    private final CustomerService service;


    @Operation(summary = "Studio: Update my profile (partial)")
    @PatchMapping(path = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    public ResponseEntity<ResponseMessage<StudioRes>> updateMyProfile(@RequestBody StudioUpdateReq req) {
        UUID userId = studioService.getCurrentUserId();
        StudioRes res = studioService.updateSelf(userId, req); // ⬅️ 일부만 수정 + 전체 응답
        return ResponseEntity.ok(ResponseMessage.success("Studio updated", res));
    }

    // ----------------------------------------------------------------------
    // 🔹 STUDIO/ADMIN: Create designer
    // ----------------------------------------------------------------------
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    @PostMapping(value = "/designer/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseMessage<DesignerResponse>> createDesigner(
            HttpServletRequest request,
            @Valid @RequestBody DesignerCreateDto req
    ) {
        var created = designerService.createDesigner(request, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Designer created successfully", created));
    }


    @Operation(
            summary = "Get designer by ID",
            description = "Returns a single designer profile (requires admin/studio permission)."
    )
    @GetMapping("/designer/{designerId}")
    public ResponseEntity<ResponseMessage<DesignerResponse>> getDesigner(
            @PathVariable UUID designerId
    ) {
        var data = designerService.getProfile(designerId);
        return ResponseEntity.ok(ResponseMessage.success("Designer fetched successfully", data));
    }



    @Operation(summary = "Delete designer (soft delete)",
            description = "Studio can delete its own designer. Marks deleted_at; portfolio untouched.")
    @PreAuthorize("hasRole('HAIR_STUDIO') or hasRole('ADMIN')")
    @DeleteMapping("/designer/delete/{id}")
    public ResponseEntity<ResponseMessage<Void>> deleteDesigner(
            HttpServletRequest request,
            @PathVariable("id") UUID designerId
    ) {
        designerService.deleteDesigner(request, designerId);
        return ResponseEntity.ok(ResponseMessage.success("Designer soft-deleted", null));
    }

    //need to fix
    @Operation(summary = "Studio: list my customers")
    @GetMapping("/customers/list")
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    public ResponseEntity<ResponseMessage<List<CustomerDetail>>> listStudioCustomers() {
        var out = service.listStudioCustomers();
        return ResponseEntity.ok(ResponseMessage.success("OK", out));
    }

    @Operation(summary = "Studio: update a customer in my studio")
    @PatchMapping("/customers/update/{customerUserId}")
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    public ResponseEntity<ResponseMessage<CustomerDetail>> updateCustomer(
            @PathVariable UUID customerUserId,
            @Valid @RequestBody CustomerProfileUpdateReq req) {
        var out = service.updateCustomerAsStudio(customerUserId, req);
        return ResponseEntity.ok(ResponseMessage.success("Updated", out));
    }

    @Operation(summary = "Studio: delete a customer in my studio")
    @DeleteMapping("/customer/delete/{customerUserId}")
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    public ResponseEntity<ResponseMessage<Void>> deleteCustomer(@PathVariable UUID customerUserId) {
        service.deleteCustomerAsStudio(customerUserId);
        return ResponseEntity.ok(ResponseMessage.success("Deleted", null));
    }


}
