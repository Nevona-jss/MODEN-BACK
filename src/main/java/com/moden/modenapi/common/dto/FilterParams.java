package com.moden.modenapi.common.dto;


import java.util.List;

public record FilterParams(
        String keyword,            // LIKE 검색용 (서비스명 부분 검색)
        List<String> serviceNames, // IN 검색용 (정확 서비스명 리스트)
        String period              // "TODAY", "WEEK", "MONTH", "ALL" 등
) {
}
