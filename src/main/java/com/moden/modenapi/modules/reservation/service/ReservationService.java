package com.moden.modenapi.modules.reservation.service;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.enums.DesignerStatus;
import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.enums.Weekday;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.consultation.model.Consultation;
import com.moden.modenapi.modules.consultation.repository.ConsultationRepository;
import com.moden.modenapi.modules.consultation.service.ConsultationService;
import com.moden.modenapi.modules.designer.model.DesignerDetail;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService extends BaseService<Reservation> {

    private final ReservationRepository reservationRepository;
    private final PaymentService paymentService;
    private final DesignerDetailRepository designerDetailRepository;
    private final ConsultationService consultationService;
    private final UserRepository userRepository;

    @Override
    protected JpaRepository<Reservation, UUID> getRepository() {
        return reservationRepository;
    }

    // ----------------------------------------------------------------------
    // Helper: LocalDate → Weekday enum
    // (Weekday enum’ingizga qarab moslashtiring)
    // ----------------------------------------------------------------------
    private Weekday toWeekday(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek(); // MONDAY(1) ... SUNDAY(7)
        return switch (dow) {
            case MONDAY    -> Weekday.MON;
            case TUESDAY   -> Weekday.TUE;
            case WEDNESDAY -> Weekday.WED;
            case THURSDAY  -> Weekday.THU;
            case FRIDAY    -> Weekday.FRI;
            case SATURDAY  -> Weekday.SAT;
            case SUNDAY    -> Weekday.SUN;
        };
    }

    // ----------------------------------------------------------------------
    // CREATE (현재 로그인된 고객 기준)
    // ----------------------------------------------------------------------
    public ReservationResponse create(UUID currentStudioId, ReservationCreateRequest req) {

        // 0) start < end basic validation
        if (req.startTime().compareTo(req.endTime()) >= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "예약 시작 시간은 종료 시간보다 빠라야 합니다."
            );
        }

        // 1) 디자이너 상세 조회 (userId + hairStudioId 기준)
        DesignerDetail designer = designerDetailRepository
                .findByUserIdAndHairStudioIdAndDeletedAtIsNull(req.designerId(), currentStudioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "해당 헤어샵의 디자이너 정보를 찾을 수 없습니다: " + req.designerId()
                ));

        // 근무 상태 확인
        if (designer.getStatus() != DesignerStatus.WORKING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "해당 디자이너는 현재 근무 상태가 아닙니다."
            );
        }

        // 휴무일 확인
        Weekday weekday = toWeekday(req.reservationDate());
        if (designer.getDaysOff() != null && designer.getDaysOff().contains(weekday)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "해당 날짜는 디자이너의 휴무일입니다."
            );
        }
        // 2) Double booking / time overlap check (same day)
        boolean exists = reservationRepository.existsOverlappingForDesigner(
                req.designerId(),
                req.reservationDate(),
                req.startTime(),
                req.endTime(),
                ReservationStatus.RESERVED
        );

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "해당 시간대에는 이미 다른 고객이 이 디자이너에게 예약을 완료했습니다."
            );
        }

        // 3) 예약 엔티티 생성 (studioId 포함)
        Reservation entity = Reservation.builder()
                .studioId(currentStudioId)      // 🔥 studioId 저장
                .customerId(req.customerId())
                .designerId(req.designerId())
                .serviceId(req.serviceId())
                .reservationDate(req.reservationDate())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .description(req.description())
                .status(ReservationStatus.RESERVED)
                .build();

        // 4) 예약 저장
        Reservation saved = reservationRepository.save(entity);

        //  yangi konsultatsiya yaratiladi
        consultationService.createPendingForReservation(saved);

        // 5) 결제 자동 생성
        paymentService.createUnpaidPaymentForReservation(saved);

        // 6) DTO 리턴
        return toDto(saved);
    }

    // ----------------------------------------------------------------------
    // UPDATE (ID 기준 일반 수정)
    // ----------------------------------------------------------------------
    public ReservationResponse update(UUID id, ReservationUpdateRequest req) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));

        if (req.customerId() != null)      reservation.setCustomerId(req.customerId());
        if (req.designerId() != null)      reservation.setDesignerId(req.designerId());
        if (req.serviceId() != null)       reservation.setServiceId(req.serviceId());
        if (req.reservationDate() != null) reservation.setReservationDate(req.reservationDate());
        if (req.startTime() != null)       reservation.setStartTime(req.startTime());
        if (req.endTime() != null)         reservation.setEndTime(req.endTime());
        if (req.description() != null)     reservation.setDescription(req.description());
        if (req.status() != null)          reservation.setStatus(req.status());

        return toDto(reservation);
    }


    // ----------------------------------------------------------------------
    // 이하 부분들에서 reservationAt 대신 date+time 사용
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public ReservationResponse get(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));
        return toDto(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listByDesigner(UUID designerId) {
        return reservationRepository.findByDesignerId(designerId).stream()
                .map(this::toDto)
                .toList();
    }

    public ReservationResponse cancel(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 ID의 예약을 찾을 수 없습니다: " + id
                ));
        reservation.setStatus(ReservationStatus.CANCELED);
        return toDto(reservation);
    }

    // *** searchDynamic: fromDate / toDate faqat sana bo‘yicha filter ***
    @Transactional(readOnly = true)
    public List<ReservationResponse> searchDynamic(
            UUID designerId,
            UUID customerId,
            UUID serviceId,
            ReservationStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer size
    ) {
        int pageIndex = (page == null || page < 1) ? 0 : page - 1;
        int pageSize  = (size == null || size < 1) ? 10 : size;

        Pageable pageable = PageRequest.of(
                pageIndex,
                pageSize,
                Sort.by(Sort.Direction.DESC, "reservationDate").and(
                        Sort.by(Sort.Direction.DESC, "startTime")
                )
        );

        List<Reservation> list = reservationRepository.searchDynamic(
                designerId,
                customerId,
                serviceId,
                status,
                fromDate,
                toDate,
                pageable
        );

        return list.stream().map(this::toDto).toList();
    }

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

    // listForDesignerFiltered / listForCustomerFiltered
    @Transactional(readOnly = true)
    public List<ReservationResponse> listForDesignerFiltered(
            UUID designerId,
            ReservationStatus status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return searchDynamic(designerId, null, null, status, fromDate, toDate);
    }


    @Transactional(readOnly = true)
    public List<ReservationResponse> listForCustomerFiltered(
            UUID customerId,
            ReservationStatus status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return searchDynamic(null, customerId, null, status, fromDate, toDate);
    }

    // ----------------------------------------------------------------------
    // ENTITY → DTO
    // ----------------------------------------------------------------------
    private ReservationResponse toDto(Reservation r) {
        var paymentStatus = paymentService.getPaymentStatusByReservationId(r.getId());

        String customerFullName = null;
        String customerPhone    = null;
        String designerFullName = null;
        String serviceName      = null;
        String paymentId        = null;

        // 1) 고객 정보 조회 (이름 + 전화번호)
        if (r.getCustomerId() != null) {
            var customerOpt = userRepository.findById(r.getCustomerId());
            if (customerOpt.isPresent()) {
                var customer = customerOpt.get();
                // field 이름은 너네 Customer 엔티티에 맞춰서 수정해줘
                customerFullName = customer.getFullName();   // 예: getFullName(), getNickname() 등
                customerPhone    = customer.getPhone();  // 예: getMobile(), getPhoneNumber() 등
            }
        }

        // 2) (원하면 여기서 designer/service 정보도 join해서 채울 수 있음)

        return new ReservationResponse(
                r.getId(),
                r.getCustomerId(),
                r.getStudioId(),
                customerFullName,
                r.getDesignerId(),
                designerFullName,
                serviceName,
                r.getReservationDate(),
                r.getStartTime(),
                r.getEndTime(),
                customerPhone,
                r.getDescription(),
                r.getStatus(),
                paymentId,
                paymentStatus,
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getDeletedAt()
        );
    }



}
