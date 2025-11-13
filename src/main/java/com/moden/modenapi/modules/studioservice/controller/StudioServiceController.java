package com.moden.modenapi.modules.studioservice.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.studioservice.dto.StudioServiceCreateRequest;
import com.moden.modenapi.modules.studioservice.dto.StudioServiceRes;
import com.moden.modenapi.modules.studioservice.dto.StudioServiceUpdateReq;
import com.moden.modenapi.modules.studioservice.service.StudioServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "HAIR STUDIO-SERVICE")
@RestController
@RequestMapping("/api/studios/services")
@RequiredArgsConstructor
public class StudioServiceController {

    private final StudioServiceService studioServiceService;

    // CREATE (faqat bitta)
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Create studio service (with used products)")
    @PostMapping("/create")
    public ResponseEntity<ResponseMessage<StudioServiceRes>> create(
            @RequestBody @Valid StudioServiceCreateRequest req
    ) {
        UUID studioId = CurrentUserUtil.currentUserId();

        StudioServiceRes res = studioServiceService.create(studioId, req);
        return ResponseEntity.ok(ResponseMessage.success("Studio service created.", res));
    }


    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Update studio service (with used products)")
    @PutMapping("/update/{serviceId}")
    public ResponseEntity<ResponseMessage<StudioServiceRes>> update(
            @PathVariable UUID serviceId,
            @RequestBody @Valid StudioServiceUpdateReq req
    ) {
        UUID studioId = CurrentUserUtil.currentUserId();
        StudioServiceRes res = studioServiceService.update(studioId, serviceId, req);
        return ResponseEntity.ok(ResponseMessage.success("Studio service updated.", res));
    }

    // DELETE
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Delete studio service")
    @DeleteMapping("/delete/{serviceId}")
    public ResponseEntity<ResponseMessage<Void>> delete(
            @PathVariable UUID serviceId
    ) {
        UUID studioId = CurrentUserUtil.currentUserId();
        studioServiceService.delete(studioId, serviceId);
        return ResponseEntity.ok(ResponseMessage.success("Studio service deleted.", null));
    }

    // DETAIL
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Get one studio service")
    @GetMapping("/get/{serviceId}")
    public ResponseEntity<ResponseMessage<StudioServiceRes>> getOne(
            @PathVariable UUID serviceId
    ) {
        UUID studioId = CurrentUserUtil.currentUserId();
        StudioServiceRes res = studioServiceService.getOne(studioId, serviceId);
        return ResponseEntity.ok(ResponseMessage.success("Studio service fetched.", res));
    }

    // LIST
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "List my studio services")
    @GetMapping("/list")
    public ResponseEntity<ResponseMessage<List<StudioServiceRes>>> listMyServices() {
        UUID studioId = CurrentUserUtil.currentUserId();
        List<StudioServiceRes> list = studioServiceService.listByStudio(studioId);
        return ResponseEntity.ok(ResponseMessage.success("Studio services fetched.", list));
    }
}
