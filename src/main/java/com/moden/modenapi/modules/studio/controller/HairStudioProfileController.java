package com.moden.modenapi.modules.studio.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.service.HairStudioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Hair Studio Profile", description = "Profile operations for hair studios")
@RestController
@RequestMapping("/api/studios")
@RequiredArgsConstructor
public class HairStudioProfileController {

    private final HairStudioService service;

    /**
     * PATCH /api/studios/{id}/profile
     * Allowed: ADMIN or the studio owner (HAIR_STUDIO)
     */

    @Operation(
            summary = "Get studio profile by login code",
            description = "Retrieve studio profile using the studio login ID (idForLogin). Example: ST-MODEN-72143"
    )
    @GetMapping("/code/{idForLogin}/profile")
    public ResponseEntity<ResponseMessage<StudioRes>> getProfileByCode(
            @Parameter(description = "Studio login ID (idForLogin)", required = true, example = "ST-MODEN-72143")
            @PathVariable String idForLogin) {

        try {
            StudioRes studio = service.get(idForLogin); // service method that finds by idForLogin
            return ResponseEntity.ok(ResponseMessage.success("Profile retrieved", studio));
        } catch (RuntimeException ex) {
            // map service "not found" (or other runtime) to 404
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Studio not found: " + idForLogin);
        }
    }
}