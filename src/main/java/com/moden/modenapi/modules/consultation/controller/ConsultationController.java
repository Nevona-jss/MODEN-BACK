package com.moden.modenapi.modules.consultation.controller;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.service.FileStorageService;
import com.moden.modenapi.modules.consultation.dto.ConsultationRes;
import com.moden.modenapi.modules.consultation.dto.ConsultationUpdateReq;
import com.moden.modenapi.modules.consultation.service.ConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "CONSULTATION", description = "고객–디자이너 상담 관리 API")
@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;

    // ----------------------------------------
    //  상담 단건 조회 (상담 ID 기준)
    // ----------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "상담 상세 조회 (상담 ID 기준)")
    @GetMapping("/get/{id}")
    public ResponseEntity<ResponseMessage<ConsultationRes>> getOne(
            @PathVariable UUID id
    ) {
        ConsultationRes res = consultationService.getOne(id);
        return ResponseEntity.ok(ResponseMessage.success("상담 상세 조회가 완료되었습니다.", res));
    }

    // ----------------------------------------
    //  상담 조회 (예약 ID 기준)
    // ----------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(summary = "상담 상세 조회 (예약 ID 기준)")
    @GetMapping("/by-reservation/{reservationId}")
    public ResponseEntity<ResponseMessage<ConsultationRes>> getByReservation(
            @PathVariable UUID reservationId
    ) {
        ConsultationRes res = consultationService.getByReservationId(reservationId);
        return ResponseEntity.ok(ResponseMessage.success("예약 기준 상담 조회가 완료되었습니다.", res));
    }

    // ----------------------------------------
    //  상담 수정 (이미지 업로드 + 디자이너 배정 가능)
    // ----------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(
            summary = "상담 수정 (URL 및 메모 수정, status는 자동 COMPLETED)"
    )
    @PatchMapping(
            value = "/update/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResponseMessage<ConsultationRes>> update(
            @PathVariable UUID id,
            @RequestBody ConsultationUpdateReq req
    ) {
        ConsultationRes res = consultationService.update(id, req);
        return ResponseEntity.ok(
                ResponseMessage.success("상담 정보가 수정되었습니다.", res)
        );
    }

    // ----------------------------------------
    //  상담 목록 (스튜디오/디자이너용) - 동적 필터
    // ----------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(
            summary = "상담 목록 조회 (필터 포함)",
            description = """
                상담 상태 및 예약/고객 정보를 기준으로 동적 조회.
                - status      : PENDING / COMPLETED ...
                - designerId  : 상담 담당 디자이너 ID (consultation.designer_id)
                - customerId  : 예약 고객 ID
                - serviceId   : 시술(서비스) ID
                - fromDate/toDate : 예약일 기준 날짜 범위 (YYYY-MM-DD)
                아무 파라미터도 안 주면 전체 상담(soft delete 제외)을 반환합니다.
                """
    )
    @GetMapping("/list")
    public ResponseEntity<ResponseMessage<List<ConsultationRes>>> list(
            @RequestParam(required = false) ConsultationStatus status,
            @RequestParam(required = false) UUID designerId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        List<ConsultationRes> list = consultationService.searchForStaff(
                designerId,
                customerId,
                serviceId,
                status,
                fromDate,
                toDate
        );
        return ResponseEntity.ok(
                ResponseMessage.success("상담 목록 조회가 완료되었습니다.", list)
        );
    }
}
