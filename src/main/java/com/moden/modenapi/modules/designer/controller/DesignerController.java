package com.moden.modenapi.modules.designer.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.customer.dto.CustomerSignUpRequest;
import com.moden.modenapi.modules.customer.service.CustomerService;
import com.moden.modenapi.modules.designer.dto.*;
import com.moden.modenapi.modules.designer.service.DesignerService;
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

@Tag(name = "DESIGNER")
@RestController
@RequestMapping("/api/designers")
@RequiredArgsConstructor
public class DesignerController {

    private final DesignerService designerService;
    private final CustomerService customerService;

    // ----------------------------------------------------------------------
    // CUSTOMER register (already existed)
    // ----------------------------------------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @PostMapping("/customers/register")
    public ResponseEntity<ResponseMessage<Void>> registerCustomer(@RequestBody CustomerSignUpRequest req) {
        customerService.customerRegister(req, "default123!");
        return ResponseEntity.ok(ResponseMessage.<Void>builder()
                .success(true)
                .message("Customer registered (studio/designer auto-assigned).")
                .build());
    }

    // ----------------------------------------------------------------------
    // GET portfolio (already existed)
    // ----------------------------------------------------------------------
    @GetMapping("/{id}/portfolio")
    public ResponseEntity<ResponseMessage<List<PortfolioItemRes>>> getPortfolio(
            @PathVariable("id") UUID designerId
    ) {
        var list = designerService.getPortfolio(designerId);
        return ResponseEntity.ok(ResponseMessage.success("OK", list));
    }

    // ----------------------------------------------------------------------
    // 🔹 DESIGNER: update own profile (bio, phone, position, status, daysOff)
    // ----------------------------------------------------------------------
    @PreAuthorize("hasRole('DESIGNER')")
    @Operation(summary = "Update my profile (Designer self-update)")
    @PatchMapping(
            value = "/profile",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResponseMessage<DesignerResponse>> updateOwnProfile(
            HttpServletRequest request,
            @RequestBody DesignerUpdateReq req
    ) {
        var updated = designerService.updateProfile(request, req);
        return ResponseEntity.ok(
                ResponseMessage.success("Designer profile updated", updated)
        );
    }

}
