package com.moden.modenapi.modules.admin;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.auth.dto.AdminSimpleSignInReq;
import com.moden.modenapi.modules.auth.dto.AuthResponse;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.studio.dto.StudioCreateReq;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.dto.StudioUpdateReq;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@Tag(name = "ADMIN", description = "System administration APIs (ADMIN role only)")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final HairStudioDetailRepository  hairStudioDetailRepository;
    private final UserRepository userRepository;




    // ------------------------- Create ---------------------------------------------
    @PostMapping(
            value = "/admin/studios/register",
            consumes = {
                    MediaType.MULTIPART_FORM_DATA_VALUE,
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.APPLICATION_OCTET_STREAM_VALUE, // ← allow octet-stream too
                    MediaType.ALL_VALUE                       // ← catch-all
            },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseMessage<StudioRes>> createStudio(
            @Valid @RequestPart("data") StudioCreateReq req,
            @RequestPart(value = "logoFile",    required = false) MultipartFile logoFile,
            @RequestPart(value = "bannerFile",  required = false) MultipartFile bannerFile,
            @RequestPart(value = "profileFile", required = false) MultipartFile profileFile
    ) {
        StudioRes result = adminService.createStudio(req, logoFile, bannerFile, profileFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Studio created successfully", result));
    }


    // ------------------------- Read one -------------------------------------------
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get a studio by ID", description = "Retrieve details of a single active studio (ADMIN only).")
    @GetMapping("/admin/studios/get{studioId}")
    public ResponseEntity<ResponseMessage<StudioRes>> getStudio(@PathVariable UUID studioId) {
        StudioRes studio = adminService.getStudio(studioId);
        return ResponseEntity.ok(ResponseMessage.success("Studio retrieved successfully", studio));
    }

    // ------------------------- List all -------------------------------------------
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all active studios", description = "Retrieve all active (non-deleted) studios (ADMIN only).")
    @GetMapping("/admin/studios/list")
    public ResponseEntity<ResponseMessage<List<StudioRes>>> getAllStudios() {
        List<StudioRes> studios = adminService.getAllStudios();
        return ResponseEntity.ok(ResponseMessage.success("Studio list retrieved successfully", studios));
    }

    @Operation(summary = "Admin: Update a studio by ID (JSON, partial)")
    @PatchMapping(path = "/admin/studios/update/{studioId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseMessage<StudioRes>> adminUpdateStudio(
            @PathVariable UUID studioId,
            @RequestBody StudioUpdateReq req
    ) {
        StudioRes res = adminService.adminUpdateStudio(studioId, req);
        return ResponseEntity.ok(ResponseMessage.success("Studio updated", res));
    }



    // ------------------------- Soft Delete ----------------------------------------
    @Operation(summary = "Soft delete a studio", description = "Marks a studio as deleted without removing it from the database (ADMIN only).")
    @DeleteMapping("/admin/studios/delete/{studioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseMessage<?>> deleteStudio(@PathVariable UUID studioId) {
        adminService.deleteStudio(studioId);
        return ResponseEntity.ok(ResponseMessage.success("Studio deleted successfully"));
    }
}
