package com.moden.modenapi.modules.admin;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.studio.dto.StudioCreateReq;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.service.HairStudioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Admin", description = "System administration APIs (ADMIN role only)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')") // ensure only admins can call these endpoints
@SecurityRequirement(name = "bearerAuth") // display auth lock in Swagger UI (optional)
public class AdminController {

    private final AdminService adminService;
    private final HairStudioService service;

    // ------------ getAllUsers ------------
    @Operation(
            summary = "List all users",
            description = "Fetch the list of all users across roles (ADMIN only)."
    )
    @GetMapping("/users")
    public ResponseEntity<ResponseMessage<?>> getAllUsers() {
        var users = adminService.getAllUsers();
        return ResponseEntity.ok(ResponseMessage.success("User list retrieved successfully", users));
    }

    // ------------ getAllReservations ------------
    @Operation(
            summary = "List all reservations",
            description = "Fetch the list of all reservations in the system (ADMIN only)."
    )
    @GetMapping("/reservations")
    public ResponseEntity<ResponseMessage<?>> getAllReservations() {
        var list = adminService.getAllReservations();
        return ResponseEntity.ok(ResponseMessage.success("Reservation list retrieved successfully", list));
    }

    // ------------ create studio ------------
    @Operation(
            summary = "Create a new hair studio (auto-registers owner)",
            description = "Creates a hair studio. **Required fields**: `name`, `businessNo`, `owner`."
    )
    @RequestBody(
            description = "Only required fields shown in example",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = StudioCreateReq.class),
                    examples = @ExampleObject(value = """
      {
        "name": "Moden Hair",
        "businessNo": "123-45-67890",
        "owner": "Alice Kim"
      }
    """)
            )
    )
    @PostMapping("/studios/register")
    public ResponseEntity<ResponseMessage<StudioRes>> create(@Valid @org.springframework.web.bind.annotation.RequestBody StudioCreateReq req) {
        var studio = service.create(req);
        URI location = URI.create("/api/studios/" + studio.id());
        return ResponseEntity.created(location)
                .body(ResponseMessage.success("Hair studio created successfully", studio));
    }

    // ------------ list studios ------------
    @Operation(summary = "List all hair studios", description = "Retrieve all registered hair studios.")
    @GetMapping("/studios")
    public ResponseEntity<ResponseMessage<List<StudioRes>>> list() {
        var studios = service.list();
        return ResponseEntity.ok(ResponseMessage.success("Studio list retrieved successfully", studios));
    }

    // ------------ get studio by id ------------
    @Operation(summary = "Get a hair studio by login code", description = "Retrieve one studio by its idForLogin (login code).")
    @GetMapping("/studios/code/{idForLogin}")
    public ResponseEntity<ResponseMessage<StudioRes>> getByCode(
            @Parameter(description = "Studio login ID (idForLogin)", required = true, example = "ST-MODEN-72143")
            @PathVariable String idForLogin) {
        var studio = service.get(idForLogin);
        return ResponseEntity.ok(ResponseMessage.success("Studio retrieved successfully", studio));
    }
}
