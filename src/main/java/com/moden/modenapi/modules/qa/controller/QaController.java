package com.moden.modenapi.modules.qa.controller;

import com.moden.modenapi.common.response.ApiResponse;
import com.moden.modenapi.modules.qa.dto.QaReportDto;
import com.moden.modenapi.modules.qa.service.QaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QaController {
    private final QaService service;

    @GetMapping public ApiResponse<?> list(){ return ApiResponse.ok(service.list()); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> create(@RequestBody QaReportDto dto){ return ApiResponse.ok(service.create(dto)); }
}
