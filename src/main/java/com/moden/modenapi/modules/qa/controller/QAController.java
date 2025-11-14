package com.moden.modenapi.modules.qa.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.qa.dto.QAAnswerRequest;
import com.moden.modenapi.modules.qa.dto.QACreateRequest;
import com.moden.modenapi.modules.qa.dto.QAResponse;
import com.moden.modenapi.modules.qa.service.QAService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Q&A", description = "1:1 Inquiry between Customer and Hair Studio")
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QAController {

    private final QAService qaService;


    // ---------------- STUDIO / DESIGNER ----------------

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "List inquiries for my studio (studio/designer)")
    @GetMapping("/list")
    public ResponseEntity<ResponseMessage<List<QAResponse>>> studioList() {
        UUID userId = CurrentUserUtil.currentUserId();
        var list = qaService.listForStudio(userId);
        return ResponseEntity.ok(ResponseMessage.success("Studio inquiries", list));
    }

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Get inquiry detail for my studio (studio/designer)")
    @GetMapping("/get/{qaId}")
    public ResponseEntity<ResponseMessage<QAResponse>> studioDetail(
            @PathVariable UUID qaId
    ) {
        UUID userId = CurrentUserUtil.currentUserId();
        var res = qaService.getForStudio(userId, qaId);
        return ResponseEntity.ok(ResponseMessage.success("Inquiry detail", res));
    }

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "Answer inquiry (studio/designer, one-shot answer)")
    @PostMapping("/{qaId}/answer")
    public ResponseEntity<ResponseMessage<QAResponse>> answer(
            @PathVariable UUID qaId,
            @Valid @RequestBody QAAnswerRequest req
    ) {
        UUID userId = CurrentUserUtil.currentUserId();
        var res = qaService.answerByStudio(userId, qaId, req);
        return ResponseEntity.ok(ResponseMessage.success("Answer saved", res));
    }
}
