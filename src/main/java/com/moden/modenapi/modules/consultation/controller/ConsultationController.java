package com.moden.modenapi.modules.consultation.controller;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.enums.PaymentStatus;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.service.FileStorageService;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.consultation.dto.ConsultationCreateReq;
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

    private final FileStorageService fileStorageService;
    private final ConsultationService consultationService;

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @PostMapping(
            value = "/create",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResponseMessage<ConsultationRes>> create(
            @RequestParam("reservationId") UUID reservationId,
            @RequestPart(value = "wantedImage", required = false) MultipartFile wantedImage,
            @RequestPart(value = "beforeImage", required = false) MultipartFile beforeImage,
            @RequestPart(value = "afterImage", required = false) MultipartFile afterImage,
            @RequestPart(value = "drawingImage", required = false) MultipartFile drawingImage,
            @RequestParam(value = "consultationMemo", required = false) String consultationMemo,
            @RequestParam(value = "customerMemo", required = false) String customerMemo
    ) {
        // 🔹 현재 로그인 사용자 (디자이너 or 스튜디오)
        UUID currentUserId = CurrentUserUtil.currentUserId();

        // 파일 -> URL 변환
        String wantedUrl = (wantedImage != null && !wantedImage.isEmpty())
                ? fileStorageService.saveFile(wantedImage)
                : null;

        String beforeUrl = (beforeImage != null && !beforeImage.isEmpty())
                ? fileStorageService.saveFile(beforeImage)
                : null;

        String afterUrl = (afterImage != null && !afterImage.isEmpty())
                ? fileStorageService.saveFile(afterImage)
                : null;

        String drawingUrl = (drawingImage != null && !drawingImage.isEmpty())
                ? fileStorageService.saveFile(drawingImage)
                : null;

        ConsultationCreateReq dto = new ConsultationCreateReq(
                reservationId,
                wantedUrl,
                beforeUrl,
                afterUrl,
                consultationMemo,
                customerMemo,
                drawingUrl
        );

        // 🔹 스튜디오/디자이너 권한 체크 포함된 서비스 메서드 호출
        ConsultationRes res = consultationService.createByStaff(currentUserId, dto);
        return ResponseEntity.ok(ResponseMessage.success("상담이 생성되었습니다.", res));
    }


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

    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(
            summary = "상담 수정 (이미지 업로드 포함)"
    )
    @PatchMapping(
            value = "/update/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResponseMessage<ConsultationRes>> update(
            @PathVariable UUID id,
            @RequestParam(value = "status", required = false) ConsultationStatus status,
            @RequestParam(value = "consultationMemo", required = false) String consultationMemo,
            @RequestParam(value = "customerMemo", required = false) String customerMemo,
            @RequestPart(value = "wantedImage", required = false) MultipartFile wantedImage,
            @RequestPart(value = "beforeImage", required = false) MultipartFile beforeImage,
            @RequestPart(value = "afterImage", required = false) MultipartFile afterImage,
            @RequestPart(value = "drawingImage", required = false) MultipartFile drawingImage
    ) {
        // 파일이 넘어온 경우에만 저장 → URL 생성
        String wantedUrl = (wantedImage != null && !wantedImage.isEmpty())
                ? fileStorageService.saveFile(wantedImage)
                : null;

        String beforeUrl = (beforeImage != null && !beforeImage.isEmpty())
                ? fileStorageService.saveFile(beforeImage)
                : null;

        String afterUrl = (afterImage != null && !afterImage.isEmpty())
                ? fileStorageService.saveFile(afterImage)
                : null;

        String drawingUrl = (drawingImage != null && !drawingImage.isEmpty())
                ? fileStorageService.saveFile(drawingImage)
                : null;

        // 서비스에서 쓰는 DTO 로 조립
        ConsultationUpdateReq req = new ConsultationUpdateReq(
                status,
                wantedUrl,
                beforeUrl,
                afterUrl,
                consultationMemo,
                customerMemo,
                drawingUrl
        );

        ConsultationRes res = consultationService.update(id, req);
        return ResponseEntity.ok(ResponseMessage.success("상담 정보가 수정되었습니다.", res));
    }

    // ----------------------------------------
//  상담 목록 (스튜디오/관리자/디자이너용) - 동적 필터
// ----------------------------------------
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @Operation(
            summary = "상담 목록 조회 (필터 포함)",
            description = """
                상담 상태 및 예약/고객 정보를 기준으로 동적 조회.
                - status      : PENDING / COMPLETED ...
                - designerId  : 예약 디자이너 ID
                - customerId  : 예약 고객 ID
                - serviceId   : 시술(서비스) ID
                - fromDate/toDate : 예약일시 기준 날짜 범위 (YYYY-MM-DD)
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
