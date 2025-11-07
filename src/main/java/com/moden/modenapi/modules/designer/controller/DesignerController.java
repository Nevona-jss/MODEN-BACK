package com.moden.modenapi.modules.designer.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.designer.dto.*;
import com.moden.modenapi.modules.designer.service.DesignerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @Operation(summary = "Update my profile (Designer only)")
    @PreAuthorize("hasRole('DESIGNER')")
    @PatchMapping("/profile")
    public ResponseEntity<ResponseMessage<DesignerResponse>> updateMyProfile(
            HttpServletRequest request,
            @Valid @RequestBody DesignerUpdateReq req
    ) {
        var out = designerService.updateOwnProfile(request, req);
        return ResponseEntity.ok(ResponseMessage.success("Profile updated", out));
    }


    @Operation(summary = "Add portfolio items (Studio owner of designer OR the designer)")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @PostMapping("/{designerId}/add/portfolio")
    public ResponseEntity<ResponseMessage<List<PortfolioItemRes>>> addPortfolio(
            HttpServletRequest request,
            @PathVariable UUID designerId,
            @Valid @RequestBody PortfolioAddReq req
    ) {
        var list = designerService.addPortfolioItems(request, designerId, req);
        return ResponseEntity.ok(ResponseMessage.success("Portfolio updated", list));
    }

    @GetMapping("/{id}/portfolio")
    public ResponseEntity<ResponseMessage<List<PortfolioItemRes>>> getPortfolio(
            @PathVariable("id") UUID designerId
    ) {
        var list = designerService.getPortfolio(designerId);
        return ResponseEntity.ok(ResponseMessage.success("OK", list));
    }

}
