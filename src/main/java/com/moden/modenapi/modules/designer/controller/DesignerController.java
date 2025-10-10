package com.moden.modenapi.modules.designer.controller;

import com.moden.modenapi.common.enums.UserType;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.auth.dto.SignUpRequest;
import com.moden.modenapi.modules.auth.service.AuthService;
import com.moden.modenapi.modules.designer.dto.DesignerProfileResponse;
import com.moden.modenapi.modules.designer.dto.ReservationSummaryRes;
import com.moden.modenapi.modules.designer.service.DesignerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller that manages all Designer-specific APIs.
 * <p>
 * Accessible only to users with {@code UserType.DESIGNER}.
 * Provides profile information and assigned reservation listings.
 */
@Tag(name = "Designer", description = "APIs related to Hair Designers")
@RestController
@RequestMapping("/api/designers")
@RequiredArgsConstructor
public class DesignerController {

    private final DesignerService designerService;
    private final AuthService authService;

    // 🔹 DESIGNER REGISTER (salon tomonidan qo'shiladi)
    @PostMapping("/register")
    public ResponseEntity<ResponseMessage<Void>> registerDesigner(@RequestBody SignUpRequest req) {
        var fixedReq = new SignUpRequest(req.name(), req.phone(), UserType.DESIGNER);
        authService.signUp(fixedReq);
        return ResponseEntity.ok(
                ResponseMessage.<Void>builder()
                        .success(true)
                        .message("Designer successfully registered.")
                        .data(null)
                        .build()
        );
    }


    /**
     * 🔹 Get designer profile by ID
     *
     * Example:
     * <pre>
     * GET /api/designers/profile/{designerId}
     * </pre>
     *
     * @param designerId UUID of the designer
     * @return Designer profile information
     */
    @Operation(
            summary = "Get designer profile",
            description = "Retrieve a designer's profile details including bio, portfolio URL, and linked salon."
    )
    @GetMapping("/profile/{designerId}")
    public ResponseEntity<ResponseMessage<DesignerProfileResponse>> getProfile(
            @PathVariable UUID designerId
    ) {
        var data = designerService.getProfile(designerId);
        return ResponseEntity.ok(
                ResponseMessage.<DesignerProfileResponse>builder()
                        .success(true)
                        .message("Designer profile retrieved successfully.")
                        .data(data)
                        .build()
        );
    }

    /**
     * 🔹 List all reservations assigned to a specific designer.
     *
     * Example:
     * <pre>
     * GET /api/designers/{designerId}/reservations
     * </pre>
     *
     * @param designerId UUID of the designer
     * @return List of reservations (date, customer, status, etc.)
     */
    @Operation(
            summary = "List designer reservations",
            description = "Fetch all active or upcoming reservations assigned to the given designer."
    )
    @GetMapping("/{designerId}/reservations")
    public ResponseEntity<ResponseMessage<List<ReservationSummaryRes>>> getReservations(
            @PathVariable UUID designerId
    ) {
        var list = designerService.getReservations(designerId);
        return ResponseEntity.ok(
                ResponseMessage.<List<ReservationSummaryRes>>builder()
                        .success(true)
                        .message("Designer reservations retrieved successfully.")
                        .data(list)
                        .build()
        );
    }
}
