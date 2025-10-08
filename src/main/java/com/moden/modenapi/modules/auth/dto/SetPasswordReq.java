package com.moden.modenapi.modules.auth.dto;

import jakarta.validation.constraints.*;
public record SetPasswordReq(@NotBlank @Size(min=8, max=200) String rawPassword) {}
