package com.moden.modenapi.modules.consultation.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ConsultationSearchReq(
        UUID customerId,
        UUID serviceId,          // eski filter (selectbox tanlagan bo‘lsa)
        String serviceNameKeyword, // ✨ yozib qidirish uchun ("펌", "컷" ...)
        String period,
        LocalDate fromDate,
        LocalDate toDate
) {}
