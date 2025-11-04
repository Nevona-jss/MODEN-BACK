package com.moden.modenapi.modules.designer.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.designer.dto.DesignerCreateDto;
import com.moden.modenapi.modules.designer.dto.DesignerResponse;
import com.moden.modenapi.modules.designer.dto.DesignerUpdateReq;
import com.moden.modenapi.modules.designer.service.DesignerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Designer", description = "APIs related to Hair Designers")
@RestController
@RequestMapping("/api/designers")
@RequiredArgsConstructor
public class DesignerController {

    private final DesignerService designerService;

    // ----------------------------------------------------------------------
    // 🔹 STUDIO/ADMIN: Create designer
    // ----------------------------------------------------------------------
    @Operation(
            summary = "Create designer (by Studio/Admin)",
            description = "Creates a new designer. `hairStudioId` is required in the request body."
    )
    @PostMapping
    public ResponseEntity<ResponseMessage<DesignerResponse>> createDesigner(
            @Valid @RequestBody DesignerCreateDto req
    ) {
        var created = designerService.createDesigner(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Designer created successfully", created));
    }

    // ----------------------------------------------------------------------
    // 🔹 DESIGNER: Update own profile
    // ----------------------------------------------------------------------
    @Operation(
            summary = "Update own profile",
            description = "Allows the logged-in designer to update bio, portfolio, phone, login ID, or position."
    )
    @PatchMapping("/update")
    public ResponseEntity<ResponseMessage<DesignerResponse>> updateProfile(
            @RequestBody DesignerUpdateReq req
    ) {
        var updated = designerService.updateProfile(
                req.bio(), req.portfolioUrl(), req.phone(), req.idForLogin(), req.position()
        );
        return ResponseEntity.ok(ResponseMessage.success("Profile updated successfully", updated));
    }

    // ----------------------------------------------------------------------
    // 🔹 DESIGNER: Get current profile (/me)
    // ----------------------------------------------------------------------
    @Operation(
            summary = "Get current designer profile",
            description = "Fetches the profile of the logged-in designer using their JWT access token."
    )
    @GetMapping("/me")
    public ResponseEntity<ResponseMessage<DesignerResponse>> getCurrentDesignerProfile() {
        var data = designerService.getCurrentDesignerProfile();
        return ResponseEntity.ok(ResponseMessage.success("Designer profile fetched successfully", data));
    }

    // ----------------------------------------------------------------------
    // 🔹 ADMIN/STUDIO: Get designer by ID
    // ----------------------------------------------------------------------
    @Operation(
            summary = "Get designer by ID",
            description = "Returns a single designer profile (requires admin/studio permission)."
    )
    @GetMapping("/{designerId}")
    public ResponseEntity<ResponseMessage<DesignerResponse>> getDesigner(
            @PathVariable UUID designerId
    ) {
        var data = designerService.getProfile(designerId);
        return ResponseEntity.ok(ResponseMessage.success("Designer fetched successfully", data));
    }

    // ----------------------------------------------------------------------
    // 🔹 ADMIN/STUDIO: List all designers by studio
    // ----------------------------------------------------------------------
    @Operation(
            summary = "List designers by studio",
            description = "Returns designers that belong to the given studio (active only)."
    )
    @GetMapping("/studios/{studioId}")
    public ResponseEntity<ResponseMessage<List<DesignerResponse>>> listByStudio(
            @PathVariable UUID studioId
    ) {
        var list = designerService.getAllDesignersByStudio(studioId);
        return ResponseEntity.ok(ResponseMessage.success("Designer list fetched successfully", list));
    }

    // ----------------------------------------------------------------------
    // 🔹 ADMIN/STUDIO: Soft delete designer
    // ----------------------------------------------------------------------
    @Operation(
            summary = "Soft delete designer",
            description = "Marks the designer as deleted (soft delete)."
    )
    @DeleteMapping("/{designerId}")
    public ResponseEntity<Void> deleteDesigner(@PathVariable UUID designerId) {
        designerService.deleteDesigner(designerId);
        return ResponseEntity.noContent().build();
    }
}
