package com.moden.modenapi.modules.reservation.service;

import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.enums.Weekday;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.consultation.service.ConsultationService;
import com.moden.modenapi.modules.designer.model.DesignerDetail;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.payment.service.PaymentService;
import com.moden.modenapi.modules.reservation.dto.ReservationCreateRequest;
import com.moden.modenapi.modules.reservation.dto.ReservationPageRes;
import com.moden.modenapi.modules.reservation.dto.ReservationResponse;
import com.moden.modenapi.modules.reservation.dto.ReservationUpdateRequest;
import com.moden.modenapi.modules.reservation.model.Reservation;
import com.moden.modenapi.modules.reservation.repository.ReservationRepository;
import com.moden.modenapi.modules.studioservice.model.StudioService;
import com.moden.modenapi.modules.studioservice.repository.StudioServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private final StudioServiceRepository studioServiceRepository;

    @Override
    protected JpaRepository<Reservation, UUID> getRepository() {
        return reservationRepository;
    }

    // ----------------------------------------------------------------------
    // Helper: LocalDate → Weekday enum
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
    // CREATE
    // ----------------------------------------------------------------------
    @Transactional
    public ReservationResponse createReservation(ReservationCreateRequest req) {

        UUID studioId = req.studioId();

        // 0) 시간 검증
        if (req.startTime().compareTo(req.endTime()) >= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "예약 시작 시간은 종료 시간보다 빨라야 합니다."
            );
        }

        // 1) 디자이너 조회 (designerId = 디자이너 userId)
        DesignerDetail designer = designerDetailRepository
                .findByUserIdAndDeletedAtIsNull(req.designerId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "디자이너 정보를 찾을 수 없습니다: " + req.designerId()
                ));

        // 디자이너가 이 샵 소속인지 확인
        if (!studioId.equals(designer.getHairStudioId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "현재 헤어샵 소속 디자이너가 아닙니다."
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

        // 2) 서비스 목록 검증
        List<UUID> serviceIds = req.serviceIds();
        if (serviceIds == null || serviceIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "최소 1개 이상의 서비스를 선택해야 합니다."
            );
        }

        // 2-1) 이 샵에 속한 서비스들만 가져오기
        List<StudioService> services =
                studioServiceRepository.findAllByStudioAndIds(studioId, serviceIds);

        if (services.size() != serviceIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "선택한 서비스 중 일부는 이 헤어샵에 존재하지 않습니다."
            );
        }

        // 2-2) 💰 총 금액 계산 (BigDecimal 로)
        BigDecimal totalAmount = services.stream()
                .map(StudioService::getServicePrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3) 중복 예약 체크
        boolean exists = reservationRepository.existsOverlappingForDesigner(
                req.designerId(),              // 디자이너 userId
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

        // 4) Reservation 엔티티 생성 (serviceIds 리스트 저장)
        Reservation entity = Reservation.builder()
                .studioId(studioId)
                .customerId(req.customerId())
                .designerId(req.designerId())
                .reservationDate(req.reservationDate())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .description(req.description())
                .status(ReservationStatus.RESERVED)
                .serviceIds(new ArrayList<>(serviceIds))  // 🔥 ID 리스트 그대로 저장
                .build();

        Reservation saved = reservationRepository.save(entity);

        // 5) 상담 생성
        consultationService.createPendingForReservation(saved);

        // 6) 결제 생성 (총액은 paymentService 안에서 다시 계산하지만, 맞춰서 넘겨도 됨)
        paymentService.createUnpaidPaymentForReservation(
                saved,
                totalAmount.doubleValue()
        );

        // 7) 응답 DTO
        return toDto(saved);
    }

    // ----------------------------------------------------------------------
    // UPDATE
    // ----------------------------------------------------------------------
    public ReservationResponse update(UUID id, ReservationUpdateRequest req) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));

        if (req.customerId() != null)      reservation.setCustomerId(req.customerId());
        if (req.designerId() != null)      reservation.setDesignerId(req.designerId());
        if (req.reservationDate() != null) reservation.setReservationDate(req.reservationDate());
        if (req.startTime() != null)       reservation.setStartTime(req.startTime());
        if (req.endTime() != null)         reservation.setEndTime(req.endTime());
        if (req.description() != null)     reservation.setDescription(req.description());
        if (req.status() != null)          reservation.setStatus(req.status());

        // 서비스 변경 허용 시
        if (req.serviceIds() != null && !req.serviceIds().isEmpty()) {
            reservation.setServiceIds(new ArrayList<>(req.serviceIds()));
        }

        return toDto(reservation);
    }

    // ----------------------------------------------------------------------
    // GET
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public ReservationResponse get(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));
        return toDto(reservation);
    }

    // ----------------------------------------------------------------------
    // LIST BY DESIGNER
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ReservationResponse> listByDesigner(UUID designerId) {
        return reservationRepository.findByDesignerId(designerId).stream()
                .map(this::toDto)
                .toList();
    }

    // ----------------------------------------------------------------------
    // CANCEL
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
// SEARCH DYNAMIC (filter + pagination + meta)
// ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public ReservationPageRes searchDynamic(
            UUID designerId,
            UUID customerId,
            UUID serviceId,
            ReservationStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer size
    ) {
        // 🔹 1) page / size 보정 (네 로직 그대로 유지)
        int pageIndex = (page == null || page < 1) ? 0 : page - 1;  // 0-based
        int limit     = (size == null || size < 1) ? 10 : size;

        Pageable pageable = PageRequest.of(
                pageIndex,
                limit,
                Sort.by(Sort.Direction.DESC, "reservationDate")
                        .and(Sort.by(Sort.Direction.DESC, "startTime"))
        );

        // 🔹 2) 현재 페이지 데이터 조회 (기존 searchDynamic 그대로)
        List<Reservation> list = reservationRepository.searchDynamic(
                designerId,
                customerId,
                serviceId,
                status,
                fromDate,
                toDate,
                pageable
        );

        // 🔹 3) ENTITY → DTO
        List<ReservationResponse> data = list.stream()
                .map(this::toDto)
                .toList();

        // 🔹 4) 전체 개수
        // 가장 좋은 건 동일한 필터로 COUNT 쿼리 하나 만드는 것:
        //   long totalCount = reservationRepository.countDynamic(...);
        //
        // 우선은 형태 맞추는 게 목적이면, 아래처럼 data.size() 써도 동작은 함
        // (이 경우 "현재 페이지 개수" = totalCount)
        long totalCount = data.size();
        // TODO: 나중에 진짜 total 원하면 countDynamic(...) 추가

        int currentPage = pageIndex + 1;  // 1-based 페이지 번호

        // 🔹 5) Page DTO 로 감싸서 리턴
        return new ReservationPageRes(
                totalCount,
                limit,
                currentPage,
                data
        );
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
        return searchDynamic(designerId, customerId, serviceId, status, fromDate, toDate, 1, 10).data();
    }

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
        String customerPhone = null;
        String designerFullName = null;
        String paymentId = null;

        // 1) 고객 정보 조회
        if (r.getCustomerId() != null) {
            var customerOpt = userRepository.findById(r.getCustomerId());
            if (customerOpt.isPresent()) {
                var customer = customerOpt.get();
                customerFullName = customer.getFullName();
                customerPhone = customer.getPhone();
            }
        }

        UUID consultationId = null;
        ConsultationStatus consultationStatus = null;
        var consultationRes = consultationService.getByReservationId(r.getId());
        if (consultationRes != null) {
            // record 라면
            consultationId = consultationRes.id();
            consultationStatus = consultationRes.status();

            // 2) 디자이너 이름
            if (r.getDesignerId() != null) {
                var designerUserOpt = userRepository.findById(r.getDesignerId());
                designerFullName = designerUserOpt.map(User::getFullName).orElse(null);
            }
        }

        // 3) serviceName 은 사용하지 않고, serviceIds 그대로 내려줌
        return new ReservationResponse(
                r.getId(),
                r.getStudioId(),
                r.getCustomerId(),
                customerFullName,
                r.getDesignerId(),
                consultationId,
                consultationStatus,
                designerFullName,
                r.getServiceIds(),
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
