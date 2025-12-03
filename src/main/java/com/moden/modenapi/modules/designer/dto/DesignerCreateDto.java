package com.moden.modenapi.modules.designer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moden.modenapi.common.enums.DesignerStatus;
import com.moden.modenapi.common.enums.Position;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DesignerCreateDto(

        @Schema(description = "Login ID for designer", example = "designer001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(min = 3, max = 50)
        @JsonProperty("idForLogin")
        String idForLogin,

        @JsonProperty("portfolioUrl")
        List<String> portfolio,

        @Size(min = 2, max = 150)
        @JsonProperty("fullName")
        String fullName,

        @NotBlank
        @Size(max = 20)
        @JsonProperty("phone")
        String phone,

        @JsonProperty("position")
        Position position,

        @Schema(description = "Login password (plain). Will be hashed.", example = "moden1234!", required = true)
        @NotBlank
        @Size(min = 8, max = 100)
        @JsonProperty("password")
        String password,

        @JsonProperty("status")
        DesignerStatus status,

        @Schema(
                description = "Days off codes (0=MONDAY ... 6=SUNDAY)",
                example = "[0, 6]"
        )
        @JsonProperty("daysOff")
        List<Integer> daysOff

) {}
