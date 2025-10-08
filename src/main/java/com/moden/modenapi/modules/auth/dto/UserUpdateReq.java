package com.moden.modenapi.modules.auth.dto;

import com.moden.modenapi.common.enums.Gender;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UserUpdateReq(
        @Size(max=100) String name,
        @Size(max=255) String email,
        LocalDate birthdate,
        Gender gender,
        boolean consentMarketing
) {}
