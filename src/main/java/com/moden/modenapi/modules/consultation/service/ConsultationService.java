package com.moden.modenapi.modules.consultation.service;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.enums.PaymentStatus;
import com.moden.modenapi.common.dto.FilterParams;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.consultation.dto.*;
import com.moden.modenapi.modules.consultation.model.Consultation;
import com.moden.modenapi.modules.consultation.repository.ConsultationRepository;
import com.moden.modenapi.modules.designer.model.DesignerDetail;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.reservation.model.Reservation;
import com.moden.modenapi.modules.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationService extends BaseService<Consultation> {

    private final ConsultationRepository consultationRepository;
    private final ReservationRepository reservationRepository;
    private final DesignerDetailRepository designerDetailRepository;

    @Override
    protected JpaRepository<Consultation, UUID> getRepository() {
        return consultationRepository;
    }

    // --------------------------------------------------------------------
    // 🔹 Reservation 생성 시 자동 상담 생성
    // --------------------------------------------------------------------
    public Consultation createPendingForReservation(Reservation reservation) {
        Consultation entity = Consultation.builder()
                .reservationId(reservation.getId())
                .designerId(null)
                .status(ConsultationStatus.PENDING)
                .build();
        return consultationRepository.save(entity);
    }

    // --------------------------------------------------------------------
    // 🔹 상담 단건 조회
    // --------------------------------------------------------------------
    @Transactional(readOnly = true)
    public ConsultationRes getOne(UUID id) {
        Consultation c = consultationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상담을 찾을 수 없습니다."));

        Reservation r = reservationRepository.findById(c.getReservationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예약 정보를 찾을 수 없습니다."));

        return toRes(c, r);
    }

    // --------------------------------------------------------------------
    // 🔹 예약 기반 조회
    // --------------------------------------------------------------------
    @Transactional(readOnly = true)
    public ConsultationRes getByReservationId(UUID reservationId) {
        Consultation c = consultationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 예약으로 상담 없음."));

        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예약 정보 없음."));

        return toRes(c, r);
    }

    // --------------------------------------------------------------------
    // 🔹 상담 수정 (컨트롤러에서 그대로 update(id, req) 사용)
    // --------------------------------------------------------------------
    public ConsultationRes update(UUID id, ConsultationUpdateReq req) {
        Consultation c = consultationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상담 없음."));

        if (req.designerId() != null) c.setDesignerId(req.designerId());
        if (req.wantedImageUrl() != null) c.setWantedImageUrl(req.wantedImageUrl());
        if (req.beforeImageUrl() != null) c.setBeforeImageUrl(req.beforeImageUrl());
        if (req.afterImageUrl() != null) c.setAfterImageUrl(req.afterImageUrl());
        if (req.consultationMemo() != null) c.setConsultationMemo(req.consultationMemo());
        if (req.customerMemo() != null) c.setCustomerMemo(req.customerMemo());
        if (req.drawingImageUrl() != null) c.setDrawingImageUrl(req.drawingImageUrl());

        c.setStatus(ConsultationStatus.COMPLETED);

        Reservation r = reservationRepository.findById(c.getReservationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예약 없음."));

        return toRes(c, r);
    }

    // --------------------------------------------------------------------
    // 🔹 FilterParams (period, serviceNames ...) 기반 목록 (고객용)
    // --------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ConsultationRes> listForCustomerFiltered(FilterParams filterParams) {

        UUID customerId = CurrentUserUtil.currentUserId();

        Specification<Consultation> spec =
                ConsultationSpecifications.fromFilterParams(customerId, filterParams);

        // 가장 최근 상담부터
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        List<Consultation> list = consultationRepository.findAll();
        return toResListWithReservations(list);
    }

    // --------------------------------------------------------------------
    // 🔹 고객 메모 업데이트
    // --------------------------------------------------------------------
    public ConsultationRes updateCustomerMemo(UUID customerId,
                                              UUID consultationId,
                                              CustomerMemoUpdateReq req) {

        Consultation c = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상담 없음."));

        Reservation r = reservationRepository.findById(c.getReservationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예약 없음."));

        if (!r.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 상담만 수정 가능.");
        }

        c.setCustomerMemo(req.customerMemo());
        return toRes(c, r);
    }

    // --------------------------------------------------------------------
    // 🔹 고객용 검색 (serviceId / serviceNameKeyword / 날짜)
    // --------------------------------------------------------------------
    public List<Consultation> search(ConsultationSearchReq req) {

        UUID serviceId = req.serviceId();
        String serviceNameKeyword = req.serviceNameKeyword();

        // serviceNameKeyword가 있으면 우선 사용
        if (serviceNameKeyword != null && !serviceNameKeyword.isBlank()) {
            return consultationRepository.searchDynamicForCustomer(
                    req.customerId(),
                    null,                 // serviceId 무시 (필요하면 둘 다 사용하는 버전으로 변경 가능)
                    serviceNameKeyword,
                    req.fromDate(),
                    req.toDate()
            );
        }

        // 아니면 serviceId 기준 검색
        return consultationRepository.searchDynamicForCustomer(
                req.customerId(),
                serviceId,
                null,
                req.fromDate(),
                req.toDate()
        );
    }

    // --------------------------------------------------------------------
    // 🔹 스튜디오에서 특정 고객 상담 조회
    // --------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ConsultationRes> listForStudioByCustomer(
            UUID customerId,
            ConsultationStatus status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Instant from = (fromDate != null)
                ? fromDate.atStartOfDay(ZoneId.of("Asia/Tashkent")).toInstant()
                : Instant.EPOCH;

        Instant to = (toDate != null)
                ? toDate.plusDays(1).atStartOfDay(ZoneId.of("Asia/Tashkent")).toInstant()
                : Instant.now();

        // ✔ Custom native query (serviceId 필터는 사용 안 하므로 null)
        List<Consultation> list = consultationRepository.findForCustomerWithFilters(
                customerId,
                null,      // serviceId
                status,
                from,
                to
        );

        return list.stream()
                .map(this::mapToDto)
                .toList();
    }

    // --------------------------------------------------------------------
    // 🔹 Reservation → 대표 서비스 이름 (serviceIds 중 첫 번째)
    // --------------------------------------------------------------------
    private String resolveServiceNameForReservation(Reservation r) {
        if (r.getServiceIds() == null || r.getServiceIds().isEmpty()) {
            return null;
        }
        // 대표 서비스 하나만 사용 (첫 번째)
        UUID mainServiceId = r.getServiceIds().get(0);
        return resolveServiceName(mainServiceId); // 기존에 있던 메서드: UUID → String
    }

    // --------------------------------------------------------------------
    // 🔹 mapToDto: 스튜디오/직원용 상세 DTO
    // --------------------------------------------------------------------
    private ConsultationRes mapToDto(Consultation c) {
        Reservation r = reservationRepository.findById(c.getReservationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Reservation not found for consultation " + c.getId()
                ));

        String studioName       = resolveStudioName(r);
        String customerName     = resolveCustomerFullName(r.getCustomerId());
        UUID designerIdForName  = (c.getDesignerId() != null) ? c.getDesignerId() : r.getDesignerId();
        String designerName     = resolveDesignerFullName(designerIdForName);
        String designerPosition = resolveDesignerPosition(designerIdForName);

        String serviceName      = resolveServiceNameForReservation(r);

        BigDecimal totalAmount  = resolveTotalPaymentAmount(r.getId());
        PaymentStatus paymentStatus = resolvePaymentStatus(r.getId());

        return new ConsultationRes(
                c.getId(),
                c.getReservationId(),

                customerName,
                designerName,
                designerPosition,
                serviceName,
                studioName,
                totalAmount,
                r.getReservationDate(),
                r.getStartTime(),
                r.getEndTime(),
                c.getStatus(),
                paymentStatus,
                c.getWantedImageUrl(),
                c.getBeforeImageUrl(),
                c.getAfterImageUrl(),
                c.getConsultationMemo(),
                c.getCustomerMemo(),
                c.getDrawingImageUrl(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getDeletedAt()
        );
    }

    private String resolveDesignerPosition(UUID designerId) {
        if (designerId == null) {
            return null;
        }
        DesignerDetail dd = designerDetailRepository
                .findById(designerId)
                .orElse(null);
        return (dd != null) ? String.valueOf(dd.getPosition()) : null;
    }

    // --------------------------------------------------------------------
    // 🔹 Mapper (기본용)
    // --------------------------------------------------------------------
    private ConsultationRes toRes(Consultation c, Reservation r) {

        String studioName       = resolveStudioName(r);
        String customerName     = resolveCustomerFullName(r.getCustomerId());
        UUID designerIdForName  = (c.getDesignerId() != null) ? c.getDesignerId() : r.getDesignerId();
        String designerName     = resolveDesignerFullName(designerIdForName);

        String serviceName      = resolveServiceNameForReservation(r);

        BigDecimal totalAmount  = resolveTotalPaymentAmount(r.getId());
        PaymentStatus paymentStatus = resolvePaymentStatus(r.getId());

        return new ConsultationRes(
                c.getId(),
                c.getReservationId(),
                customerName,
                designerName,
                null,
                serviceName,
                studioName,
                totalAmount,
                r.getReservationDate(),
                r.getStartTime(),
                r.getEndTime(),
                c.getStatus(),
                paymentStatus,
                c.getWantedImageUrl(),
                c.getBeforeImageUrl(),
                c.getAfterImageUrl(),
                c.getConsultationMemo(),
                c.getCustomerMemo(),
                c.getDrawingImageUrl(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getDeletedAt()
        );
    }

    // --------------------------------------------------------------------
    // 🔹 리스트 변환
    // --------------------------------------------------------------------
    private List<ConsultationRes> toResListWithReservations(List<Consultation> list) {
        if (list.isEmpty()) return List.of();

        List<UUID> reservationIds =
                list.stream().map(Consultation::getReservationId).toList();

        Map<UUID, Reservation> map =
                reservationRepository.findAllById(reservationIds)
                        .stream()
                        .collect(Collectors.toMap(Reservation::getId, r -> r));

        return list.stream()
                .map(c -> toRes(c, map.get(c.getReservationId())))
                .toList();
    }

    // --------------------------------------------------------------------
    // 🔹 직원용 검색 (Specification 기반)
    // --------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ConsultationRes> searchForStaff(
            UUID designerId,
            UUID customerId,
            UUID serviceId,
            ConsultationStatus status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Specification<Consultation> spec = ConsultationSpecifications.forStaff(
                designerId, customerId, serviceId, status, fromDate, toDate
        );

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        List<Consultation> list = consultationRepository.findAll();
        return toResListWithReservations(list);
    }

    // --------------------------------------------------------------------
    // 🔹 디자이너 기준 목록
    // --------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ConsultationRes> listForDesigner(UUID designerId) {
        // 1) 디자이너의 예약들
        List<Reservation> reservations = reservationRepository.findByDesignerId(designerId);
        if (reservations.isEmpty()) return List.of();

        Map<UUID, Reservation> reservationMap = reservations.stream()
                .collect(Collectors.toMap(Reservation::getId, r -> r));

        List<UUID> reservationIds = reservations.stream()
                .map(Reservation::getId)
                .toList();

        // 2) 해당 예약들에 대한 상담들
        List<Consultation> consultations =
                consultationRepository.findByReservationIdIn(reservationIds);

        return consultations.stream()
                .map(c -> {
                    Reservation r = reservationMap.get(c.getReservationId());
                    return toRes(c, r);
                })
                .toList();
    }

    // --------------------------------------------------------------------
    // 🔹 Placeholder resolver
    // --------------------------------------------------------------------
    private PaymentStatus resolvePaymentStatus(UUID reservationId) { return PaymentStatus.PENDING; }
    private BigDecimal resolveTotalPaymentAmount(UUID reservationId) { return BigDecimal.ZERO; }
    private String resolveCustomerFullName(UUID customerId) { return "고객이름"; }
    private String resolveDesignerFullName(UUID designerId) { return "디자이너"; }
    private String resolveServiceName(UUID serviceId) { return "서비스"; }
    private String resolveStudioName(Reservation r) { return "스튜디오"; }
}
