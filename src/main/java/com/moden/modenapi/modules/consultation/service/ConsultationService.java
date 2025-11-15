package com.moden.modenapi.modules.consultation.service;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.enums.PaymentStatus;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.consultation.dto.ConsultationCreateReq;
import com.moden.modenapi.modules.consultation.dto.ConsultationRes;
import com.moden.modenapi.modules.consultation.dto.ConsultationUpdateReq;
import com.moden.modenapi.modules.consultation.dto.CustomerMemoUpdateReq;
import com.moden.modenapi.modules.consultation.model.Consultation;
import com.moden.modenapi.modules.consultation.repository.ConsultationRepository;
import com.moden.modenapi.modules.reservation.model.Reservation;
import com.moden.modenapi.modules.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationService extends BaseService<Consultation> {

    private final ConsultationRepository consultationRepository;
    private final ReservationRepository reservationRepository;
    // TODO: payment / user / designer / service / studio 모듈 연결 시 여기에 의존성 추가

    @Override
    protected JpaRepository<Consultation, UUID> getRepository() {
        return consultationRepository;
    }

    // ---------------------------
    //  상담 생성 (예약 기반)
    // ---------------------------
    public ConsultationRes create(ConsultationCreateReq req) {
        Reservation reservation = reservationRepository.findById(req.reservationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 예약을 찾을 수 없습니다: " + req.reservationId()
                ));

        Consultation entity = Consultation.builder()
                .reservationId(req.reservationId())
                .status(ConsultationStatus.COMPLETED)
                .paymentStatus(resolvePaymentStatus(req.reservationId()))
                .wantedImageUrl(req.wantedImageUrl())
                .beforeImageUrl(req.beforeImageUrl())
                .afterImageUrl(req.afterImageUrl())
                .consultationMemo(req.consultationMemo())
                .customerMemo(req.customerMemo())
                .drawingImageUrl(req.drawingImageUrl())
                .build();

        Consultation saved = consultationRepository.save(entity);
        return toRes(saved, reservation);
    }

    // ---------------------------
    //  상담 단건 조회 (ID 기준)
    // ---------------------------
    @Transactional(readOnly = true)
    public ConsultationRes getOne(UUID id) {
        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 상담을 찾을 수 없습니다: " + id
                ));

        Reservation reservation = reservationRepository.findById(consultation.getReservationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 상담에 연결된 예약을 찾을 수 없습니다: " + consultation.getReservationId()
                ));

        return toRes(consultation, reservation);
    }

    // ---------------------------
    //  예약 ID 기준 상담 조회
    // ---------------------------
    @Transactional(readOnly = true)
    public ConsultationRes getByReservationId(UUID reservationId) {
        Consultation consultation = consultationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 예약으로 생성된 상담이 없습니다: " + reservationId
                ));

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 예약을 찾을 수 없습니다: " + reservationId
                ));

        return toRes(consultation, reservation);
    }

    // ---------------------------
    //  상담 수정 (디자이너/스튜디오용)
    // ---------------------------
    public ConsultationRes update(UUID id, ConsultationUpdateReq req) {
        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 상담을 찾을 수 없습니다: " + id
                ));

        if (req.status() != null) {
            consultation.setStatus(req.status());
        }
        if (req.wantedImageUrl() != null) {
            consultation.setWantedImageUrl(req.wantedImageUrl());
        }
        if (req.beforeImageUrl() != null) {
            consultation.setBeforeImageUrl(req.beforeImageUrl());
        }
        if (req.afterImageUrl() != null) {
            consultation.setAfterImageUrl(req.afterImageUrl());
        }
        if (req.consultationMemo() != null) {
            consultation.setConsultationMemo(req.consultationMemo());
        }
        if (req.customerMemo() != null) {
            consultation.setCustomerMemo(req.customerMemo());
        }
        if (req.drawingImageUrl() != null) {
            consultation.setDrawingImageUrl(req.drawingImageUrl());
        }

        Reservation reservation = reservationRepository.findById(consultation.getReservationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 상담에 연결된 예약을 찾을 수 없습니다: " + consultation.getReservationId()
                ));

        return toRes(consultation, reservation);
    }

    // ---------------------------
    //  상태별 상담 목록 (스튜디오/관리자용)
    // ---------------------------
    @Transactional(readOnly = true)
    public List<ConsultationRes> listByStatus(ConsultationStatus status) {
        List<Consultation> consultations = consultationRepository.findByStatus(status);
        return toResListWithReservations(consultations);
    }

    // ---------------------------
    //  디자이너별 상담 목록 (Reservation.designerId 기준)
    // ---------------------------
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

    // ========================================================
    //  👤 고객 전용: 내 상담 목록 / 필터
    // ========================================================

    /** 현재 고객의 모든 상담 목록 */
    @Transactional(readOnly = true)
    public List<ConsultationRes> listForCustomerAll(UUID customerId) {
        List<Reservation> reservations = reservationRepository.findByCustomerId(customerId);
        if (reservations.isEmpty()) return List.of();

        Map<UUID, Reservation> reservationMap = reservations.stream()
                .collect(Collectors.toMap(Reservation::getId, r -> r));
        List<UUID> reservationIds = reservations.stream()
                .map(Reservation::getId)
                .toList();

        List<Consultation> consultations =
                consultationRepository.findByReservationIdIn(reservationIds);

        return consultations.stream()
                .map(c -> {
                    Reservation r = reservationMap.get(c.getReservationId());
                    return toRes(c, r);
                })
                .toList();
    }

    /** 오늘 상담 목록 (고객 기준) */
    @Transactional(readOnly = true)
    public List<ConsultationRes> listForCustomerToday(UUID customerId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<Reservation> reservations = reservationRepository
                .findByCustomerIdAndReservationAtBetween(customerId, start, end);
        return mapConsultationsByReservations(reservations);
    }

    /** 이번 주 상담 목록 (고객 기준) */
    @Transactional(readOnly = true)
    public List<ConsultationRes> listForCustomerThisWeek(UUID customerId) {
        LocalDate any = LocalDate.now();
        int dayOfWeek = any.getDayOfWeek().getValue(); // 1=Mon ... 7=Sun
        LocalDate monday = any.minusDays(dayOfWeek - 1L);
        LocalDate nextMonday = monday.plusWeeks(1);

        LocalDateTime start = monday.atStartOfDay();
        LocalDateTime end = nextMonday.atStartOfDay();

        List<Reservation> reservations = reservationRepository
                .findByCustomerIdAndReservationAtBetween(customerId, start, end);
        return mapConsultationsByReservations(reservations);
    }

    /** 이번 달 상담 목록 (고객 기준) */
    @Transactional(readOnly = true)
    public List<ConsultationRes> listForCustomerThisMonth(UUID customerId) {
        LocalDate now = LocalDate.now();
        YearMonth ym = YearMonth.of(now.getYear(), now.getMonthValue());
        LocalDate firstDay = ym.atDay(1);
        LocalDate firstDayNextMonth = ym.plusMonths(1).atDay(1);

        LocalDateTime start = firstDay.atStartOfDay();
        LocalDateTime end = firstDayNextMonth.atStartOfDay();

        List<Reservation> reservations = reservationRepository
                .findByCustomerIdAndReservationAtBetween(customerId, start, end);
        return mapConsultationsByReservations(reservations);
    }

    /** 서비스별 필터 (현재 고객 + 특정 서비스 ID) */
    @Transactional(readOnly = true)
    public List<ConsultationRes> listForCustomerByService(UUID customerId, UUID serviceId) {
        List<Reservation> reservations = reservationRepository.findByCustomerId(customerId).stream()
                .filter(r -> serviceId.equals(r.getServiceId()))
                .toList();

        return mapConsultationsByReservations(reservations);
    }

    // ========================================================
    //  👤 고객 전용: 내 메모 업데이트
    // ========================================================

    public ConsultationRes updateCustomerMemo(UUID customerId,
                                              UUID consultationId,
                                              CustomerMemoUpdateReq req) {

        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 상담을 찾을 수 없습니다: " + consultationId
                ));

        // 상담이 연결된 예약 확인
        Reservation reservation = reservationRepository.findById(consultation.getReservationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 상담에 연결된 예약을 찾을 수 없습니다: " + consultation.getReservationId()
                ));

        // 현재 로그인한 고객이 이 예약의 주인인지 검증
        if (!reservation.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "본인에게 속한 상담에만 메모를 작성/수정할 수 있습니다."
            );
        }

        // 실제로는 고객 메모만 수정
        consultation.setCustomerMemo(req.customerMemo());

        return toRes(consultation, reservation);
    }

    // ========================================================
    //  내부 공통 매핑 유틸
    // ========================================================

    private List<ConsultationRes> mapConsultationsByReservations(List<Reservation> reservations) {
        if (reservations.isEmpty()) return List.of();

        Map<UUID, Reservation> reservationMap = reservations.stream()
                .collect(Collectors.toMap(Reservation::getId, r -> r));
        List<UUID> reservationIds = reservations.stream()
                .map(Reservation::getId)
                .toList();

        List<Consultation> consultations =
                consultationRepository.findByReservationIdIn(reservationIds);

        return consultations.stream()
                .map(c -> {
                    Reservation r = reservationMap.get(c.getReservationId());
                    return toRes(c, r);
                })
                .toList();
    }

    /** 결제 상태 조회 placeholder – Payment 모듈 붙일 때 구현 */
    private PaymentStatus resolvePaymentStatus(UUID reservationId) {
        // TODO: Payment 모듈에서 reservationId 기준 결제 상태 조회
        return PaymentStatus.PENDING;
    }

    /** 🔹 총 결제 금액 조회 placeholder */
    private BigDecimal resolveTotalPaymentAmount(UUID reservationId) {
        // TODO: Payment 모듈에서 reservationId 기준 결제 합계 조회
        return BigDecimal.ZERO; // 임시
    }

    private String resolveCustomerFullName(UUID customerId) {
        // TODO: 고객/유저 모듈에서 fullName 조회
        return "고객이름";
    }

    private String resolveDesignerFullName(UUID designerId) {
        // TODO: 디자이너 모듈에서 fullName 조회
        return "디자이너이름";
    }

    private String resolveServiceName(UUID serviceId) {
        // TODO: 서비스(시술) 모듈에서 서비스명 조회
        return "서비스이름";
    }

    private String resolveStudioName(Reservation r) {
        // TODO: 스튜디오 이름을 Reservation 또는 Studio 모듈에서 조회
        return "스튜디오이름";
    }

    // 🔹 Consultation + Reservation -> ConsultationRes 매핑
    private ConsultationRes toRes(Consultation c, Reservation r) {
        if (r == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "상담에 연결된 예약 정보가 없습니다. reservationId=" + c.getReservationId()
            );
        }

        String studioName   = resolveStudioName(r);                 // 스튜디오 이름
        String customerName = resolveCustomerFullName(r.getCustomerId());  // 고객 이름
        String designerName = resolveDesignerFullName(r.getDesignerId());  // 디자이너 이름
        String serviceName  = resolveServiceName(r.getServiceId());        // 서비스 이름
        LocalDateTime reservationAt = r.getReservationAt();               // 예약 시간
        BigDecimal totalPayment = resolveTotalPaymentAmount(r.getId());   // 총 금액

        return new ConsultationRes(
                c.getId(),
                c.getReservationId(),

                // 🔹 DTO 필드 순서에 맞춰서 넣기
                customerName,   // customerFullName
                designerName,   // designerFullName
                serviceName,    // serviceName
                studioName,     // name (스튜디오 이름)

                totalPayment,
                reservationAt,
                c.getStatus(),
                c.getPaymentStatus(),
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

    // 상태별 목록에서 사용하는 공통 헬퍼
    private List<ConsultationRes> toResListWithReservations(List<Consultation> consultations) {
        if (consultations.isEmpty()) return List.of();

        List<UUID> reservationIds = consultations.stream()
                .map(Consultation::getReservationId)
                .toList();

        List<Reservation> reservations = reservationRepository.findAllById(reservationIds);
        Map<UUID, Reservation> reservationMap = reservations.stream()
                .collect(Collectors.toMap(Reservation::getId, r -> r));

        return consultations.stream()
                .map(c -> {
                    Reservation r = reservationMap.get(c.getReservationId());
                    return toRes(c, r);
                })
                .toList();
    }

    // ---------------------------
//  상담 생성 (예약 기반 + 권한 체크)
//  - HAIR_STUDIO: 모든 예약에 대해 생성 가능
//  - DESIGNER   : 자기에게 배정된 예약만 생성 가능
// ---------------------------
    public ConsultationRes createByStaff(UUID currentUserId, ConsultationCreateReq req) {
        Reservation reservation = reservationRepository.findById(req.reservationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 예약을 찾을 수 없습니다: " + req.reservationId()
                ));

        // 현재 로그인 사용자가 HAIR_STUDIO 인지 여부
        boolean isHairStudio = hasRole("ROLE_HAIR_STUDIO");
        boolean isDesigner   = hasRole("ROLE_DESIGNER");

        // 디자이너인 경우에만 본인 예약인지 검사
        if (isDesigner && !isHairStudio) {
            UUID designerId = reservation.getDesignerId();
            if (designerId == null || !designerId.equals(currentUserId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "디자이너는 본인에게 배정된 예약에 대해서만 상담을 생성할 수 있습니다."
                );
            }
        }
        // HAIR_STUDIO 는 별도 체크 없이 통과

        Consultation entity = Consultation.builder()
                .reservationId(req.reservationId())
                .status(ConsultationStatus.PENDING)          // 상담대기
                .paymentStatus(resolvePaymentStatus(req.reservationId()))
                .wantedImageUrl(req.wantedImageUrl())
                .beforeImageUrl(req.beforeImageUrl())
                .afterImageUrl(req.afterImageUrl())
                .consultationMemo(req.consultationMemo())
                .customerMemo(req.customerMemo())
                .drawingImageUrl(req.drawingImageUrl())
                .build();

        Consultation saved = consultationRepository.save(entity);
        return toRes(saved, reservation);
    }

    /** 현재 Authentication 에 특정 ROLE 이 있는지 확인하는 유틸 */
    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) return false;

        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (role.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }


}
