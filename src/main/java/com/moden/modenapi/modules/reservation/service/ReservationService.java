package com.moden.modenapi.modules.reservation.service;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.payment.service.PaymentService;
import com.moden.modenapi.modules.reservation.dto.ReservationCreateRequest;
import com.moden.modenapi.modules.reservation.dto.ReservationResponse;
import com.moden.modenapi.modules.reservation.dto.ReservationUpdateRequest;
import com.moden.modenapi.modules.reservation.model.Reservation;
import com.moden.modenapi.modules.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService extends BaseService<Reservation> {

    private final ReservationRepository reservationRepository;
    private final PaymentService paymentService;

    @Override
    protected JpaRepository<Reservation, UUID> getRepository() {
        return reservationRepository;
    }

    // ----------------------------------------------------------------------
    // CREATE (현재 로그인된 고객 기준)
    // ----------------------------------------------------------------------
    public ReservationResponse createForCustomer(UUID customerId, ReservationCreateRequest req) {

        // 더블 예약 체크 (해당 디자이너 + 해당 시간 + 상태가 RESERVED 인 예약 존재 여부)
        boolean exists = reservationRepository
                .existsByDesignerIdAndReservationAtAndStatus(
                        req.designerId(),
                        req.reservationAt(),
                        ReservationStatus.RESERVED
                );

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "해당 시간에는 이미 다른 고객이 이 디자이너에게 예약을 완료했습니다."
            );
        }

        Reservation entity = Reservation.builder()
                .customerId(customerId)
                .designerId(req.designerId())
                .serviceId(req.serviceId())
                .reservationAt(req.reservationAt())
                .description(req.description())
                .status(ReservationStatus.RESERVED)
                // .consultationStatus(ConsultationStatus.WAITING) // 필요하면 기본값 세팅
                .build();

        // 1) 예약 저장
        Reservation saved = reservationRepository.save(entity);

        // 2) 예약 기준으로 UNPAID payment 자동 생성
        paymentService.createUnpaidPaymentForReservation(saved);

        // 3) 예약 DTO 리턴
        return toDto(saved);
    }

    // ----------------------------------------------------------------------
    // UPDATE (ID 기준 일반 수정)
    // ----------------------------------------------------------------------
    public ReservationResponse update(UUID id, ReservationUpdateRequest req) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));

        if (req.customerId() != null) reservation.setCustomerId(req.customerId());
        if (req.designerId() != null) reservation.setDesignerId(req.designerId());
        if (req.serviceId() != null) reservation.setServiceId(req.serviceId());
        if (req.reservationAt() != null) reservation.setReservationAt(req.reservationAt());
        if (req.description() != null) reservation.setDescription(req.description());
        // 필요하면 status / consultationStatus 도 여기서 반영

        return toDto(reservation);
    }

    // ----------------------------------------------------------------------
    // 단건 조회
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public ReservationResponse get(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));
        return toDto(reservation);
    }

    // ----------------------------------------------------------------------
    // 특정 디자이너의 모든 예약 목록 (단순 리스트)
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ReservationResponse> listByDesigner(UUID designerId) {
        return reservationRepository.findByDesignerId(designerId).stream()
                .map(this::toDto)
                .toList();
    }

    // ----------------------------------------------------------------------
    // 예약 취소 (관리자/공용)
    // ----------------------------------------------------------------------
    public ReservationResponse cancel(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 ID의 예약을 찾을 수 없습니다: " + id
                ));

        reservation.setStatus(ReservationStatus.CANCELED);
        return toDto(reservation);
    }

    // ----------------------------------------------------------------------
    // 고객 권한으로 본인 예약 수정/취소
    // ----------------------------------------------------------------------
    public ReservationResponse updateByCustomer(UUID customerId,
                                                UUID reservationId,
                                                ReservationUpdateRequest req) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 예약을 찾을 수 없습니다: " + reservationId
                ));

        // 본인 예약인지 확인
        if (!reservation.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "본인에게 속한 예약만 수정할 수 있습니다."
            );
        }

        if (req.customerId() != null) {
            reservation.setCustomerId(req.customerId());
        }
        if (req.designerId() != null) reservation.setDesignerId(req.designerId());
        if (req.serviceId() != null) reservation.setServiceId(req.serviceId());
        if (req.reservationAt() != null) reservation.setReservationAt(req.reservationAt());
        if (req.description() != null) reservation.setDescription(req.description());

        return toDto(reservation);
    }

    public ReservationResponse cancelByCustomer(UUID customerId, UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 예약을 찾을 수 없습니다: " + reservationId
                ));

        if (!reservation.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "본인에게 속한 예약만 취소할 수 있습니다."
            );
        }

        reservation.setStatus(ReservationStatus.CANCELED);
        return toDto(reservation);
    }

    // ----------------------------------------------------------------------
    // 디자이너 권한으로 본인 예약 수정/취소
    // ----------------------------------------------------------------------
    public ReservationResponse updateByDesigner(UUID designerId,
                                                UUID reservationId,
                                                ReservationUpdateRequest req) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 예약을 찾을 수 없습니다: " + reservationId
                ));

        // 디자이너 본인에게 배정된 예약인지 확인
        if (!reservation.getDesignerId().equals(designerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "본인에게 배정된 예약만 수정할 수 있습니다."
            );
        }

        // 디자이너가 수정 가능한 필드만 반영 (policy 에 따라 조정 가능)
        if (req.serviceId() != null) reservation.setServiceId(req.serviceId());
        if (req.reservationAt() != null) reservation.setReservationAt(req.reservationAt());
        if (req.description() != null) reservation.setDescription(req.description());

        return toDto(reservation);
    }

    public ReservationResponse cancelByDesigner(UUID designerId, UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 예약을 찾을 수 없습니다: " + reservationId
                ));

        if (!reservation.getDesignerId().equals(designerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "본인에게 배정된 예약만 취소할 수 있습니다."
            );
        }

        reservation.setStatus(ReservationStatus.CANCELED);
        return toDto(reservation);
    }

    // ----------------------------------------------------------------------
    // DYNAMIC SEARCH (pagination) – ✅ serviceId 포함
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ReservationResponse> searchDynamic(
            UUID designerId,
            UUID customerId,
            UUID serviceId,         // ✅ 추가
            ReservationStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer size
    ) {
        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to   = (toDate != null) ? toDate.plusDays(1).atStartOfDay() : null;

        int pageIndex = (page == null || page < 1) ? 0 : page - 1;   // client 1-based → 0-based
        int pageSize  = (size == null || size < 1) ? 10 : size;      // default 10

        Pageable pageable = PageRequest.of(
                pageIndex,
                pageSize,
                Sort.by(Sort.Direction.DESC, "reservationAt")
        );

        return reservationRepository.searchDynamic(
                        designerId,
                        customerId,
                        serviceId,     // ✅ repo 호출에도 포함
                        status,
                        from,
                        to,
                        pageable
                )
                .stream()
                .map(this::toDto)
                .toList();
    }

    // page/size 안 넘기는 기존 코드용 OVERLOAD – ✅ serviceId 포함
    @Transactional(readOnly = true)
    public List<ReservationResponse> searchDynamic(
            UUID designerId,
            UUID customerId,
            UUID serviceId,
            ReservationStatus status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return searchDynamic(designerId, customerId, serviceId, status, fromDate, toDate, 1, 10);
    }

    // ----------------------------------------------------------------------
    // DESIGNER FILTERED LIST
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ReservationResponse> listForDesignerFiltered(
            UUID designerId,
            ReservationStatus status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return searchDynamic(
                designerId,
                null,          // customerId filter 없음
                null,          // serviceId filter 없음
                status,
                fromDate,
                toDate
        );
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listForDesignerFiltered(
            UUID designerId,
            ReservationStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer size
    ) {
        return searchDynamic(
                designerId,
                null,
                null,          // serviceId filter 없음
                status,
                fromDate,
                toDate,
                page,
                size
        );
    }

    // ----------------------------------------------------------------------
    // CUSTOMER FILTERED LIST
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ReservationResponse> listForCustomerFiltered(
            UUID customerId,
            ReservationStatus status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return searchDynamic(
                null,          // designerId filter 없음
                customerId,
                null,          // serviceId filter 없음
                status,
                fromDate,
                toDate
        );
    }

    // ----------------------------------------------------------------------
    // ENTITY → DTO 변환
    // ----------------------------------------------------------------------
    private ReservationResponse toDto(Reservation r) {
        var paymentStatus = paymentService.getPaymentStatusByReservationId(r.getId());

        String customerFullName = null;
        String designerFullName = null;
        String serviceName      = null;
        String paymentId        = null;

        return new ReservationResponse(
                r.getId(),
                r.getCustomerId(),
                customerFullName,
                r.getDesignerId(),
                designerFullName,
                serviceName,
                r.getReservationAt(),
                r.getDescription(),
                r.getStatus(),
                r.getConsultationStatus(),
                paymentId,
                paymentStatus,
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getDeletedAt()
        );
    }

}
