package com.moden.modenapi.modules.auth.dto;

import com.moden.modenapi.common.enums.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record UserCreateReq(
        @NotBlank @Size(max=100) String name,
        @NotBlank @Size(max=20) String phone,
        @Email @Size(max=255) String email,
        LocalDate birthdate,
        Gender gender,
        @NotNull UserType userType,
        boolean consentMarketing,
        @Size(max=255) String naverId
) {}
