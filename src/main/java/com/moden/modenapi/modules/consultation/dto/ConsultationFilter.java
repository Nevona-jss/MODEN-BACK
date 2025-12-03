// src/main/java/com/moden/modenapi/modules/consultation/dto/ConsultationFilter.java
package com.moden.modenapi.modules.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "상담 검색 필터 (고객용)")
public record ConsultationFilter(

        @Schema(description = "검색 키워드 (서비스명 등 부분검색)")
        String keyword,

        @Schema(description = "서비스 이름 리스트 (여러 시술명으로 필터링할 때 사용)")
        List<String> serviceNames,

        @Schema(description = "기간 필터 (TODAY / WEEK / MONTH / ALL)", example = "WEEK")
        String period

) {
}
