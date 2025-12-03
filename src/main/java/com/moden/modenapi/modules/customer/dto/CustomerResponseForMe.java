package com.moden.modenapi.modules.customer.dto;

import com.moden.modenapi.common.enums.Gender;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.modules.studio.dto.StudioBrief;

import java.time.LocalDate;
import java.util.UUID;


public record CustomerResponseForMe(
        UUID id,
        String fullName,
        String phone,
        Role role,
        String email,
        Gender gender,
        String designerName,
        LocalDate birthdate,
        String address,
        String profileImageUrl,
        String visitReason,
        boolean consentMarketing,
        StudioBrief studioInfo,
        String lastServiceName,
        LocalDate lastVisitDate

) {}
