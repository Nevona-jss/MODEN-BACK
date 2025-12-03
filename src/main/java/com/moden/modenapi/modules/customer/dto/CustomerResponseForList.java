package com.moden.modenapi.modules.customer.dto;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponseForList(
        UUID id,
        String fullName,
        String phone,
        LocalDateTime lastVisitAt
) {
}
