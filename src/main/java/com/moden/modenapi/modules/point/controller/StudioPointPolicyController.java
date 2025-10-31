package com.moden.modenapi.modules.point.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.point.dto.StudioPointPolicyReq;
import com.moden.modenapi.modules.point.dto.StudioPointPolicyRes;
import com.moden.modenapi.modules.point.service.StudioPointPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Studio Point Policy", description = "Manage cashback/point rate for studios")
@RestController
@RequestMapping("/api/studios/{studioId}/point-policy")
@RequiredArgsConstructor
public class StudioPointPolicyController {

    private final StudioPointPolicyService service;

    // 🔹 Create or update studio’s point policy
    @Operation(summary = "Set or update point rate")
    @PostMapping
    public ResponseEntity<ResponseMessage<StudioPointPolicyRes>> setPolicy(
            @PathVariable UUID studioId,
            @Valid @RequestBody StudioPointPolicyReq req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Point policy saved successfully", service.setPolicy(studioId, req)));
    }

    // 🔹 Get studio’s point policy
    @Operation(summary = "Get current point policy")
    @GetMapping
    public ResponseEntity<ResponseMessage<StudioPointPolicyRes>> getPolicy(
            @PathVariable UUID studioId
    ) {
        return ResponseEntity.ok(ResponseMessage.success("Point policy fetched successfully", service.getPolicy(studioId)));
    }
}
