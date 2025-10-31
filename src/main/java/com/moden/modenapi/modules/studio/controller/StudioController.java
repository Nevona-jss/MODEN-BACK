package com.moden.modenapi.modules.studio.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.designer.dto.DesignerCreateDto;
import com.moden.modenapi.modules.designer.dto.DesignerResponse;
import com.moden.modenapi.modules.designer.service.DesignerService;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.dto.StudioUpdateReq;
import com.moden.modenapi.modules.studio.service.HairStudioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Hair Studio", description = "Endpoints for managing hair studios and designers")
@RestController
@RequestMapping("/api/studios")
@RequiredArgsConstructor
public class StudioController {

    private final HairStudioService studioService;
    private final DesignerService designerService;

    // ----------------------------------------------------------------------
    // 🔹 UPDATE STUDIO PROFILE (with image upload)
    // ----------------------------------------------------------------------
    @Operation(
            summary = "Update studio profile",
            description = "Updates non-deleted studio profile fields, including logo/banner/profile images"
    )
    @PatchMapping(value = "/{studioId}", consumes = {"multipart/form-data"})
    public ResponseEntity<ResponseMessage<StudioRes>> updateStudio(
            @PathVariable UUID studioId,
            @RequestPart(value = "data") @Valid StudioUpdateReq req,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestPart(value = "bannerFile", required = false) MultipartFile bannerFile,
            @RequestPart(value = "profileFile", required = false) MultipartFile profileFile
    ) {
        var updated = studioService.updateStudio(studioId, req, logoFile, bannerFile, profileFile);
        return ResponseEntity.ok(ResponseMessage.success("Studio profile updated successfully", updated));
    }

    // ----------------------------------------------------------------------
    // 🔹 GET CURRENT STUDIO (JWT)
    // ----------------------------------------------------------------------
    @Operation(
            summary = "Get current studio profile (JWT)",
            description = "Returns profile of the currently logged-in studio"
    )
    @GetMapping("/me")
    public ResponseEntity<ResponseMessage<StudioRes>> getCurrentStudio() {
        var studio = studioService.getCurrentStudio();
        return ResponseEntity.ok(ResponseMessage.success("Current studio profile retrieved successfully", studio));
    }

    // ----------------------------------------------------------------------
    // 🔹 CREATE DESIGNER (Studio only)
    // ----------------------------------------------------------------------
    @Operation(summary = "Create a new designer under this studio")
    @PostMapping("/{studioId}/designer/create")
    public ResponseEntity<ResponseMessage<DesignerResponse>> createDesigner(
            @PathVariable UUID studioId,
            @Valid @RequestBody DesignerCreateDto req
    ) {
        var created = designerService.createDesigner(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Designer created successfully", created));
    }

    // ----------------------------------------------------------------------
    // 🔹 GET DESIGNER BY ID
    // ----------------------------------------------------------------------
    @GetMapping("/designer/read/{designerId}")
    public ResponseEntity<ResponseMessage<DesignerResponse>> getDesigner(@PathVariable UUID designerId) {
        var designer = designerService.getProfile(designerId);
        return ResponseEntity.ok(ResponseMessage.success("Designer retrieved successfully", designer));
    }

    // ----------------------------------------------------------------------
    // 🔹 LIST ALL DESIGNERS UNDER STUDIO
    // ----------------------------------------------------------------------
    @GetMapping("/{studioId}/designers")
    public ResponseEntity<ResponseMessage<List<DesignerResponse>>> getAllDesigners(@PathVariable UUID studioId) {
        var list = designerService.getAllDesignersByStudio(studioId);
        return ResponseEntity.ok(ResponseMessage.success("Designer list retrieved successfully", list));
    }

    // ----------------------------------------------------------------------
    // 🔹 DELETE DESIGNER
    // ----------------------------------------------------------------------
    @DeleteMapping("/designer/delete/{designerId}")
    public ResponseEntity<ResponseMessage<String>> deleteDesigner(@PathVariable UUID designerId) {
        designerService.deleteDesigner(designerId);
        return ResponseEntity.ok(ResponseMessage.success("Designer deleted successfully"));
    }
}
