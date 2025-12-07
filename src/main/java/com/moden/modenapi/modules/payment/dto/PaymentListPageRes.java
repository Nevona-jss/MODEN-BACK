package com.moden.modenapi.modules.payment.dto;


import java.util.List;

public record PaymentListPageRes(
        long totalCount,
        int limit,
        int page,                     // 1-based page
        List<PaymentListItemRes> data // 실제 목록
) {}
