package com.moden.modenapi.modules.qa.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.qa.dto.QaReportDto;
import com.moden.modenapi.modules.qa.model.QaReport;
import com.moden.modenapi.modules.qa.service.QaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "QA Reports", description = "APIs for managing QA test and verification reports.")
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QaController {

    private final QaService service;

    @Operation(summary = "List all QA reports")
    @GetMapping
    public ResponseEntity<ResponseMessage<List<QaReport>>> list() {
        var data = service.list();
        return ResponseEntity.ok(ResponseMessage.success("QA reports retrieved successfully", data));
    }

    @Operation(summary = "Create a new QA report")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ResponseMessage<QaReport>> create(@RequestBody QaReportDto dto) {
        var data = service.create(dto);
        return ResponseEntity.ok(ResponseMessage.success("QA report created successfully", data));
    }
}
