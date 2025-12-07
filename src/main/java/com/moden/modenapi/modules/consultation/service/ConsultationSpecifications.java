package com.moden.modenapi.modules.consultation.service;

import com.moden.modenapi.common.dto.FilterParams;
import com.moden.modenapi.common.enums.ConsultationStatus;
import com.moden.modenapi.modules.consultation.dto.ConsultationFilter;
import com.moden.modenapi.modules.consultation.model.Consultation;
import com.moden.modenapi.modules.reservation.model.Reservation;
import com.moden.modenapi.modules.studioservice.model.StudioService;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConsultationSpecifications {

    private static final ZoneId ZONE = ZoneId.of("Asia/Tashkent");

    /**
     * CUSTOMER tarafdan filter:
     *  - customerId (majburiy)
     *  - keyword (ixtiyoriy, 서비스명 LIKE 검색)
     *  - serviceNames (ixtiyoriy, IN 검색)
     *  - period (TODAY/WEEK/MONTH/ALL)
     */
    public static Specification<Consultation> filter(UUID customerId, ConsultationFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 항상 현재 고객 기준
            predicates.add(cb.equal(root.get("customerId"), customerId));

            if (filter != null) {

                // 1) keyword (서비스명 LIKE 검색)
                if (filter.keyword() != null && !filter.keyword().isBlank()) {
                    Join<Consultation, StudioService> serviceJoin =
                            root.join("service", JoinType.LEFT);
                    String pattern = "%" + filter.keyword().toLowerCase() + "%";
                    predicates.add(
                            cb.like(
                                    cb.lower(serviceJoin.get("serviceName")),
                                    pattern
                            )
                    );
                }

                // 2) 여러 serviceNames 로 검색 (IN)
                if (filter.serviceNames() != null && !filter.serviceNames().isEmpty()) {
                    Join<Consultation, StudioService> serviceJoin =
                            root.join("service", JoinType.LEFT);

                    CriteriaBuilder.In<String> inClause =
                            cb.in(cb.lower(serviceJoin.get("serviceName")));

                    for (String name : filter.serviceNames()) {
                        if (name != null && !name.isBlank()) {
                            inClause.value(name.toLowerCase());
                        }
                    }

                    predicates.add(inClause);
                }

                // 3) period (TODAY / WEEK / MONTH / ALL)
                if (filter.period() != null && !filter.period().isBlank()) {
                    Instant from = resolvePeriod(filter.period());
                    if (from != null) {
                        predicates.add(
                                cb.greaterThanOrEqualTo(root.get("createdAt"), from)
                        );
                    }
                    // "ALL" bo‘lsa resolvePeriod null qaytaradi → date filter qo‘shilmaydi
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** period 문자열을 기준 Instant 로 변환 (createdAt 기준) */
    private static Instant resolvePeriod(String period) {
        if (period == null) return null;
        String p = period.trim().toUpperCase();
        LocalDate today = LocalDate.now(ZONE);
        return switch (p) {
            case "TODAY" -> today.atStartOfDay(ZONE).toInstant();
            case "WEEK" -> today.with(DayOfWeek.MONDAY).atStartOfDay(ZONE).toInstant();
            case "MONTH" -> today.withDayOfMonth(1).atStartOfDay(ZONE).toInstant();
            case "ALL" -> null;     // 전체: 날짜 필터 없음
            default -> null;
        };
    }

    /** FilterParams → ConsultationFilter → filter(customerId, cf) */
    public static Specification<Consultation> fromFilterParams(UUID customerId, FilterParams f) {
        if (f == null) {
            return filter(customerId, null);
        }

        ConsultationFilter cf = new ConsultationFilter(
                f.keyword(),
                f.serviceNames(),
                f.period()
        );

        return filter(customerId, cf);
    }

    public static Specification<Consultation> forStaff(
            UUID designerId,
            UUID customerId,
            UUID serviceId,
            ConsultationStatus status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Consultation → Reservation 조인
            Join<Consultation, Reservation> reservationJoin =
                    root.join("reservationId"); // 여기는 실제 매핑 필드명에 맞게 수정 필요
            // 만약 @ManyToOne Reservation reservation; 로 매핑해 두었다면 "reservation" 으로

            if (designerId != null) {
                predicates.add(cb.equal(reservationJoin.get("designerId"), designerId));
            }
            if (customerId != null) {
                predicates.add(cb.equal(reservationJoin.get("customerId"), customerId));
            }

            // 🔥 serviceIds ElementCollection 조인
            if (serviceId != null) {
                Join<Reservation, UUID> serviceIdsJoin = reservationJoin.join("serviceIds");
                predicates.add(cb.equal(serviceIdsJoin, serviceId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        reservationJoin.get("reservationDate"), fromDate
                ));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        reservationJoin.get("reservationDate"), toDate
                ));
            }

            // soft delete 제외
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
