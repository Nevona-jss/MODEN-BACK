package com.moden.modenapi.modules.studio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "StudioCreateReq", description = "Create studio (minimal)")
public record StudioCreateReq(
        @NotBlank @Size(max = 100)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Studio display name")
        String fullName,

        @NotBlank @Size(max = 100)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Business registration number")
        String businessNo,

        @NotBlank @Size(max = 50)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Owner phone")
        String ownerPhone,

        @NotBlank @Size(min = 6, max = 100)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Initial password for owner login")
        String password
) {}
