package com.moden.modenapi.modules.customer.dto;

import com.moden.modenapi.common.enums.Gender;
import com.moden.modenapi.common.enums.Role;

import java.time.LocalDate;
import java.util.UUID;

public record CustomerSignUpRes(

        UUID id,
        String fullName,
        String phone,
        Role role,
        String email,
        Gender gender,
        UUID designerId,
        LocalDate birthdate,
        String address,
        String visitReason

) {
}
