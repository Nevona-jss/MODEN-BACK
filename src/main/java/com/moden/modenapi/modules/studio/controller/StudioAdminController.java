package com.moden.modenapi.modules.studio.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.admin.AdminService;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.studio.dto.StudioCreateReq;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.dto.StudioUpdateReq;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "Admin Studio", description = "ADMIN creates/manages studios")
@RestController
@RequestMapping("/api/admin/studios")
@RequiredArgsConstructor
public class StudioAdminController {

    private final AdminService adminService;
    private final HairStudioDetailRepository studioRepo;
    private final UserRepository userRepo;

    @Operation(
            summary = "Create a new hair studio (minimal)",
            description = """
        Required JSON fields (inside `data`): fullName, businessNo, ownerPhone, password.
        Files are optional (logoFile, bannerFile, profileFile) — you may also upload later via update.
        """
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseMessage<StudioRes>> createStudio(
            @Valid @RequestPart("data") StudioCreateReq data,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestPart(value = "bannerFile", required = false) MultipartFile bannerFile,
            @RequestPart(value = "profileFile", required = false) MultipartFile profileFile
    ) {
        var res = adminService.createStudio(data, logoFile, bannerFile, profileFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Studio created successfully", res));
    }


    // ------------------------------------------------------------------
    // UPDATE (files optional)
    // ------------------------------------------------------------------
    @Operation(summary = "Update studio (ADMIN)")
    @PatchMapping(path = "/{studioId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseMessage<StudioRes>> updateStudio(
            @PathVariable UUID studioId,
            @RequestPart("data") StudioUpdateReq req,
            @RequestPart(value = "logoFile", required = false)    MultipartFile logoFile,
            @RequestPart(value = "bannerFile", required = false)  MultipartFile bannerFile,
            @RequestPart(value = "profileFile", required = false) MultipartFile profileFile
    ) {
        HairStudioDetail s = studioRepo.findById(studioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Studio not found"));
        if (s.getDeletedAt() != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Studio deleted");

        if (req.studioPhone() != null) s.setStudioPhone(req.studioPhone());
        if (req.address() != null)     s.setAddress(req.address());
        if (req.description() != null) s.setDescription(req.description());
        if (req.parkingInfo() != null) s.setParkingInfo(req.parkingInfo());
        if (req.instagram() != null)   s.setInstagramUrl(req.instagram());
        if (req.naver() != null)       s.setNaverUrl(req.naver());
        if (req.kakao() != null)       s.setKakaoUrl(req.kakao());
        if (req.latitude() != null)    s.setLatitude(req.latitude());
        if (req.longitude() != null)   s.setLongitude(req.longitude());

        // (파일 업로드 처리 시 서비스로 위임 가능)

        s.setUpdatedAt(Instant.now());
        studioRepo.save(s);

        var owner = userRepo.findById(s.getUserId()).orElse(null);
        String ownerFullName = owner != null ? owner.getFullName() : null;
        String ownerPhone    = owner != null ? owner.getPhone()    : null;

        var res = new StudioRes(
                // required
                s.getId(),
                s.getUserId(),
                ownerFullName,
                ownerPhone,
                // optional (order must match StudioRes)
                s.getIdForLogin(),
                s.getBusinessNo(),
                s.getOwnerName(),
                s.getStudioPhone(),
                s.getAddress(),
                s.getDescription(),
                s.getProfileImageUrl(),
                s.getLogoImageUrl(),
                s.getBannerImageUrl(),
                s.getInstagramUrl(),
                s.getNaverUrl(),
                s.getKakaoUrl(),
                s.getParkingInfo(),
                s.getLatitude(),
                s.getLongitude()
        );
        return ResponseEntity.ok(ResponseMessage.success("Studio updated", res));
    }

    @Operation(summary = "Get studio by ID (ADMIN)")
    @GetMapping("/{studioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseMessage<StudioRes>> getStudio(@PathVariable UUID studioId) {
        HairStudioDetail s = studioRepo.findById(studioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Studio not found"));

        var owner = userRepo.findById(s.getUserId()).orElse(null);
        String ownerFullName = owner != null ? owner.getFullName() : null;
        String ownerPhone    = owner != null ? owner.getPhone()    : null;

        var res = new StudioRes(
                // required
                s.getId(),
                s.getUserId(),
                ownerFullName,
                ownerPhone,
                // optional
                s.getIdForLogin(),
                s.getBusinessNo(),
                s.getOwnerName(),
                s.getStudioPhone(),
                s.getAddress(),
                s.getDescription(),
                s.getProfileImageUrl(),
                s.getLogoImageUrl(),
                s.getBannerImageUrl(),
                s.getInstagramUrl(),
                s.getNaverUrl(),
                s.getKakaoUrl(),
                s.getParkingInfo(),
                s.getLatitude(),
                s.getLongitude()
        );
        return ResponseEntity.ok(ResponseMessage.success(res));
    }

    @Operation(summary = "Soft delete studio (ADMIN)")
    @DeleteMapping("/{studioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStudio(@PathVariable UUID studioId) {
        HairStudioDetail s = studioRepo.findById(studioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Studio not found"));
        if (s.getDeletedAt() != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Studio already deleted");
        s.setDeletedAt(Instant.now());
        studioRepo.save(s);
        return ResponseEntity.noContent().build();
    }
}
