package com.moden.modenapi.modules.studio.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.studio.dto.StudioCreateReq;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.service.HairStudioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Hair Studio", description = "Endpoints for managing hair studios and their owners")
@RestController
@RequestMapping("/api/studios")
@RequiredArgsConstructor
public class HairStudioController {

    private final HairStudioService service;

    /**
     * 🔹 Create a new hair studio (automatically registers owner as HAIR_STUDIO user)
     */
    @Operation(summary = "Create a new hair studio (includes owner auto-registration)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ResponseMessage<StudioRes>> create(@RequestBody StudioCreateReq req) {
        var studio = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Hair studio created successfully", studio));
    }

    /**
     * 🔹 Get list of all hair studios
     */
    @Operation(summary = "List all hair studios")
    @GetMapping
    public ResponseEntity<ResponseMessage<List<StudioRes>>> list() {
        var studios = service.list();
        return ResponseEntity.ok(
                ResponseMessage.success("Studio list retrieved successfully", studios)
        );
    }

    /**
     * 🔹 Get single hair studio by ID
     */
    @Operation(summary = "Get a single hair studio by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseMessage<StudioRes>> get(@PathVariable UUID id) {
        var studio = service.get(id);
        return ResponseEntity.ok(
                ResponseMessage.success("Studio retrieved successfully", studio)
        );
    }
}
