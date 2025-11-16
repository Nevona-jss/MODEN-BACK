package com.moden.modenapi.common.controller;

import com.moden.modenapi.common.dto.UniversalSearchItemRes;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.service.UniversalSearchService;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;


@Tag(name = "UNIVERSAL SEARCH")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class UniversalSearchController {

    private final UniversalSearchService universalSearchService;

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "내 스튜디오 통합 검색 (서비스 + 상품 등)")
    @GetMapping
    public ResponseEntity<ResponseMessage<List<UniversalSearchItemRes>>> universalSearch(
            @RequestParam( required = false) String keyword
    ) {
        UUID studioId = CurrentUserUtil.currentUserId();

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.ok(
                    ResponseMessage.success("검색어가 비어 있어 빈 결과를 반환합니다.", Collections.emptyList())
            );
        }

        List<UniversalSearchItemRes> list =
                universalSearchService.searchForStudio(studioId, keyword);

        return ResponseEntity.ok(
                ResponseMessage.success("통합 검색이 완료되었습니다.", list)
        );
    }
}
