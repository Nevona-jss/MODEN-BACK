package com.moden.modenapi.modules.auth.dto;

import com.moden.modenapi.common.enums.Gender;
import com.moden.modenapi.common.enums.UserType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Simplified user response for admin or management APIs.
 */
public record UserResponse(
        UUID id,
        String name,
        String phone,
        String email,
        UserType userType,
        Gender gender,
        LocalDate birthdate,
        boolean consentMarketing
) {}
