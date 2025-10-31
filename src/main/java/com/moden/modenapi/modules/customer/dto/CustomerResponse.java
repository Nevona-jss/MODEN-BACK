package com.moden.modenapi.modules.customer.dto;

import com.moden.modenapi.common.enums.Role;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String fullName,
        String phone,
        Role role,
        String email,
        String gender,
        LocalDate birthdate,
        String address,
        String profileImageUrl,
        String visitReason,
        boolean consentMarketing,
        UUID designerId
) {}
