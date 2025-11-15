package com.moden.modenapi.modules.reservation.service;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.reservation.dto.ReservationCreateRequest;
import com.moden.modenapi.modules.reservation.dto.ReservationResponse;
import com.moden.modenapi.modules.reservation.dto.ReservationUpdateRequest;
import com.moden.modenapi.modules.reservation.model.Reservation;
import com.moden.modenapi.modules.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService extends BaseService<Reservation> {

    private final ReservationRepository reservationRepository;

    @Override
    protected JpaRepository<Reservation, UUID> getRepository() {
        return reservationRepository;
    }

    // ---------- CREATE (현재 로그인된 고객 기준) ----------

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
                .build();

        Reservation saved = reservationRepository.save(entity);
        return toDto(saved);
    }

    // ---------- UPDATE (ID 기준 일반 수정) ----------

    public ReservationResponse update(UUID id, ReservationUpdateRequest req) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));

        if (req.customerId() != null) reservation.setCustomerId(req.customerId());
        if (req.designerId() != null) reservation.setDesignerId(req.designerId());
        if (req.serviceId() != null) reservation.setServiceId(req.serviceId());
        if (req.reservationAt() != null) reservation.setReservationAt(req.reservationAt());
        if (req.description() != null) reservation.setDescription(req.description());

        return toDto(reservation);
    }

    // ---------- 단건 조회 ----------

    @Transactional(readOnly = true)
    public ReservationResponse get(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));
        return toDto(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> list() {
        return reservationRepository.findAll().stream()
                .map(this::toDto)   // 이미 아래에 있는 toDto() 재사용
                .toList();
    }




    // ---------- 일간 / 주간 / 월간 전체 예약 조회 ----------

    @Transactional(readOnly = true)
    public List<ReservationResponse> listDaily(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        return reservationRepository.findByReservationAtBetween(start, end).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listWeekly(LocalDate anyDateInWeek) {
        // 해당 날짜가 포함된 주의 월요일을 구한다
        int dayOfWeek = anyDateInWeek.getDayOfWeek().getValue(); // 1=Mon ... 7=Sun
        LocalDate monday = anyDateInWeek.minusDays(dayOfWeek - 1L);
        LocalDate nextMonday = monday.plusWeeks(1);

        LocalDateTime start = monday.atStartOfDay();
        LocalDateTime end = nextMonday.atStartOfDay();

        return reservationRepository.findByReservationAtBetween(start, end).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listMonthly(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate firstDay = ym.atDay(1);
        LocalDate firstDayNextMonth = ym.plusMonths(1).atDay(1);

        LocalDateTime start = firstDay.atStartOfDay();
        LocalDateTime end = firstDayNextMonth.atStartOfDay();

        return reservationRepository.findByReservationAtBetween(start, end).stream()
                .map(this::toDto)
                .toList();
    }



    // 1) 특정 고객의 모든 예약 목록
    @Transactional(readOnly = true)
    public List<ReservationResponse> listByCustomer(UUID customerId) {
        return reservationRepository.findByCustomerId(customerId).stream()
                .map(this::toDto)
                .toList();
    }

    // 2) 특정 디자이너의 모든 예약 목록
    @Transactional(readOnly = true)
    public List<ReservationResponse> listByDesigner(UUID designerId) {
        return reservationRepository.findByDesignerId(designerId).stream()
                .map(this::toDto)
                .toList();
    }

    // 4) 상태 기준 예약 목록 (예: RESERVED 만)
    @Transactional(readOnly = true)
    public List<ReservationResponse> listByStatus(ReservationStatus status) {
        return reservationRepository.findByStatus(status).stream()
                .map(this::toDto)
                .toList();
    }

    // ---------- 일반 취소 (ID 기준) ----------

    public ReservationResponse cancel(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 ID의 예약을 찾을 수 없습니다: " + id
                ));

        // ✅ 상태를 자동으로 CANCELED 로 변경
        reservation.setStatus(ReservationStatus.CANCELED);

        return toDto(reservation);
    }

    // ---------- 디자이너별 조회 (디자이너 본인 기준 ALL) ----------

    @Transactional(readOnly = true)
    public List<ReservationResponse> listForDesignerAll(UUID designerId) {
        return reservationRepository.findByDesignerId(designerId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listForDesignerByStatus(UUID designerId, ReservationStatus status) {
        return reservationRepository.findByDesignerIdAndStatus(designerId, status).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listForDesignerRange(
            UUID designerId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return reservationRepository
                .findByDesignerIdAndReservationAtBetween(designerId, from, to).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listForDesignerDaily(UUID designerId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        return reservationRepository
                .findByDesignerIdAndReservationAtBetween(designerId, start, end).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listForDesignerWeekly(UUID designerId, LocalDate anyDateInWeek) {
        int dayOfWeek = anyDateInWeek.getDayOfWeek().getValue(); // 1=Mon ... 7=Sun
        LocalDate monday = anyDateInWeek.minusDays(dayOfWeek - 1L);
        LocalDate nextMonday = monday.plusWeeks(1);

        LocalDateTime start = monday.atStartOfDay();
        LocalDateTime end = nextMonday.atStartOfDay();

        return reservationRepository
                .findByDesignerIdAndReservationAtBetween(designerId, start, end).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listForDesignerMonthly(UUID designerId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate firstDay = ym.atDay(1);
        LocalDate firstDayNextMonth = ym.plusMonths(1).atDay(1);

        LocalDateTime start = firstDay.atStartOfDay();
        LocalDateTime end = firstDayNextMonth.atStartOfDay();

        return reservationRepository
                .findByDesignerIdAndReservationAtBetween(designerId, start, end).stream()
                .map(this::toDto)
                .toList();
    }

    // ---------- 고객 권한으로 본인 예약 수정/취소 ----------

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
            // 일반적으로 customerId 변경은 권장하지 않지만, 필요 시 허용 가능
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

    // ---------- 디자이너 권한으로 본인 예약 수정/취소 ----------

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

        // customerId / designerId 는 디자이너 권한으로는 변경하지 않음
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

    /** 현재 고객 기준 - 상태별(RESERVED / CANCELED) 예약 목록 */
    @Transactional(readOnly = true)
    public List<ReservationResponse> listForCustomerByStatus(UUID customerId, ReservationStatus status) {
        return reservationRepository.findByCustomerIdAndStatus(customerId, status).stream()
                .map(this::toDto)
                .toList();
    }

    /** 현재 고객 기준 - 기간(from/to) 내 예약 목록 */
    @Transactional(readOnly = true)
    public List<ReservationResponse> listForCustomerRange(
            UUID customerId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return reservationRepository
                .findByCustomerIdAndReservationAtBetween(customerId, from, to).stream()
                .map(this::toDto)
                .toList();
    }

    /** 현재 고객 기준 - 일별 예약 목록 */
    @Transactional(readOnly = true)
    public List<ReservationResponse> listForCustomerDaily(UUID customerId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        return reservationRepository
                .findByCustomerIdAndReservationAtBetween(customerId, start, end).stream()
                .map(this::toDto)
                .toList();
    }

    /** 현재 고객 기준 - 주간 예약 목록 (해당 날짜가 포함된 주) */
    @Transactional(readOnly = true)
    public List<ReservationResponse> listForCustomerWeekly(UUID customerId, LocalDate anyDateInWeek) {
        int dayOfWeek = anyDateInWeek.getDayOfWeek().getValue(); // 1=Mon ... 7=Sun
        LocalDate monday = anyDateInWeek.minusDays(dayOfWeek - 1L);
        LocalDate nextMonday = monday.plusWeeks(1);

        LocalDateTime start = monday.atStartOfDay();
        LocalDateTime end = nextMonday.atStartOfDay();

        return reservationRepository
                .findByCustomerIdAndReservationAtBetween(customerId, start, end).stream()
                .map(this::toDto)
                .toList();
    }

    /** 현재 고객 기준 - 월별 예약 목록 (year + month) */
    @Transactional(readOnly = true)
    public List<ReservationResponse> listForCustomerMonthly(UUID customerId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate firstDay = ym.atDay(1);
        LocalDate firstDayNextMonth = ym.plusMonths(1).atDay(1);

        LocalDateTime start = firstDay.atStartOfDay();
        LocalDateTime end = firstDayNextMonth.atStartOfDay();

        return reservationRepository
                .findByCustomerIdAndReservationAtBetween(customerId, start, end).stream()
                .map(this::toDto)
                .toList();
    }

    // ---------- Entity → DTO 변환 ----------

    private ReservationResponse toDto(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getCustomerId(),
                r.getDesignerId(),
                r.getServiceId(),
                r.getReservationAt(),
                r.getDescription(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getDeletedAt()
        );
    }


}
