package com.moden.modenapi.modules.designer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moden.modenapi.common.enums.DesignerStatus;
import com.moden.modenapi.common.enums.Position;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DesignerCreateDto(

        @JsonProperty("bio")
        String bio,

        @JsonProperty("portfolioUrl")
        String portfolioUrl,

        // optional fullName
        @Size(min = 2, max = 150)
        @JsonProperty("fullName")
        String fullName,

        // required phone
        @NotBlank @Size(max = 20)
        @JsonProperty("phone")
        String phone,

        // optional enums
        @JsonProperty("position")
        Position position,

        @Schema(description = "Login password (plain). Will be hashed.", example = "moden1234!", required = true)
        @NotBlank @Size(min = 8, max = 100)
        @JsonProperty("password")
        String password,

        @JsonProperty("status")
        DesignerStatus status
) {}
