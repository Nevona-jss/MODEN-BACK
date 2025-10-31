package com.moden.modenapi.modules.admin;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.studio.dto.StudioCreateReq;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin", description = "System administration APIs (ADMIN role only)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')") // ✅ 권한 접두어 ROLE_ 필수
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    // ----------------------------------------------------------------------
    // 🔹 CREATE STUDIO
    // ----------------------------------------------------------------------
    @Operation(
            summary = "Create a new hair studio (auto-registers ownerName)",
            description = """
                    Creates a new hair studio associated with a user ID.
                    Required fields: `name`, `businessNo`, `ownerName`, and `password`.
                    You can also upload logo/banner/profile images.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Studio created successfully",
                    content = @Content(schema = @Schema(implementation = StudioRes.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Duplicate studio ID")
    })
    @PostMapping("/studios/create/{userId}")
    public ResponseEntity<ResponseMessage<StudioRes>> createStudio(
            @PathVariable UUID userId,
            @Valid @RequestPart("data") StudioCreateReq req,       // ✅ JSON 필드
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestPart(value = "bannerFile", required = false) MultipartFile bannerFile,
            @RequestPart(value = "profileFile", required = false) MultipartFile profileFile
    ) {
        StudioRes result = adminService.createStudio(userId, req, logoFile, bannerFile, profileFile);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Studio created successfully", result));
    }

    // ----------------------------------------------------------------------
    // 🔹 GET SINGLE STUDIO
    // ----------------------------------------------------------------------
    @Operation(summary = "Get a studio by ID", description = "Retrieve details of a single active studio (ADMIN only).")
    @GetMapping("/studios/read/{studioId}") // ✅ {studioId} 괄호 누락 수정
    public ResponseEntity<ResponseMessage<StudioRes>> getStudio(@PathVariable UUID studioId) {
        StudioRes studio = adminService.getStudio(studioId);
        return ResponseEntity.ok(ResponseMessage.success("Studio retrieved successfully", studio));
    }

    // ----------------------------------------------------------------------
    // 🔹 GET ALL STUDIOS
    // ----------------------------------------------------------------------
    @Operation(summary = "List all active studios", description = "Retrieve all active (non-deleted) studios (ADMIN only).")
    @GetMapping("/studios/list")
    public ResponseEntity<ResponseMessage<List<StudioRes>>> getAllStudios() {
        List<StudioRes> studios = adminService.getAllStudios();
        return ResponseEntity.ok(ResponseMessage.success("Studio list retrieved successfully", studios));
    }

    // ----------------------------------------------------------------------
    // 🔹 DELETE STUDIO (SOFT DELETE)
    // ----------------------------------------------------------------------
    @Operation(summary = "Soft delete a studio", description = "Marks a studio as deleted without removing it from the database (ADMIN only).")
    @DeleteMapping("/studios/delete/{studioId}") // ✅ 괄호 누락 수정
    public ResponseEntity<ResponseMessage<?>> deleteStudio(@PathVariable UUID studioId) {
        adminService.deleteStudio(studioId);
        return ResponseEntity.ok(ResponseMessage.success("Studio deleted successfully"));
    }

    // ----------------------------------------------------------------------
    // 🔹 USERS (Future Extension)
    // ----------------------------------------------------------------------
    // 필요 시 getAllUsers(), getAllReservations() 추가
}
