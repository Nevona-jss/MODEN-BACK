package com.moden.modenapi.modules.auth.dto;

import com.moden.modenapi.common.enums.*;
import java.time.LocalDate;
import java.util.UUID;

public record UserRes(
        UUID id, String name, String phone, String email,
        LocalDate birthdate, Gender gender, UserType userType,
        boolean consentMarketing, String naverId
) {}
