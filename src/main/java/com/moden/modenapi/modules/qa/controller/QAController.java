package com.moden.modenapi.modules.qa.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.qa.dto.QACreateRequest;
import com.moden.modenapi.modules.qa.dto.QAResponse;
import com.moden.modenapi.modules.qa.service.QAService;
import com.moden.modenapi.security.JwtProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Q&A", description = "1:1 Inquiry between Customer and Hair Studio")
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QAController {

    private final QAService qaService;
    private final JwtProvider jwtProvider;

    // ----------------------------------------------------------------------
    // 🔹 Create inquiry (Customer)
    // ----------------------------------------------------------------------
    @Operation(summary = "Create inquiry", description = "Customer posts a new question to a studio")
    @PostMapping("/create")
    public ResponseEntity<ResponseMessage<QAResponse>> createQuestion(
            @Valid @RequestBody QACreateRequest req,
            HttpServletRequest request
    ) {
        UUID userId = extractUserId(request);
        QAResponse created = qaService.createQuestion(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("문의가 등록되었습니다.", created));
    }

    // ----------------------------------------------------------------------
    // 🔹 Answer inquiry (Studio)
    // ----------------------------------------------------------------------
    @Operation(summary = "Answer inquiry", description = "Studio writes an answer to a customer’s question")
    @PostMapping("/answer/{qaId}")
    public ResponseEntity<ResponseMessage<QAResponse>> answerQuestion(
            @PathVariable UUID qaId,
            @RequestParam String answer,
            HttpServletRequest request
    ) {
        UUID studioUserId = extractUserId(request);
        QAResponse updated = qaService.answerQuestion(studioUserId, qaId, answer);
        return ResponseEntity.ok(ResponseMessage.success("답변이 등록되었습니다.", updated));
    }

    // ----------------------------------------------------------------------
    // 🔹 Get all my inquiries (Customer)
    // ----------------------------------------------------------------------
    @Operation(summary = "Get my inquiries", description = "Fetch all inquiries created by the current user")
    @GetMapping("/my")
    public ResponseEntity<ResponseMessage<List<QAResponse>>> getMyQuestions(HttpServletRequest request) {
        UUID userId = extractUserId(request);
        List<QAResponse> list = qaService.getUserQuestions(userId);
        return ResponseEntity.ok(ResponseMessage.success("내 문의 내역이 조회되었습니다.", list));
    }

    // ----------------------------------------------------------------------
    // 🔹 Helper: Extract user ID from JWT
    // ----------------------------------------------------------------------
    private UUID extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7).trim();
        if (!jwtProvider.validateToken(token)) {
            throw new RuntimeException("Invalid or expired JWT token");
        }
        return UUID.fromString(jwtProvider.getUserId(token));
    }
}
