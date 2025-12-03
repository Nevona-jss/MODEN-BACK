package com.moden.modenapi.modules.customer.service;

import com.moden.modenapi.common.enums.ReservationStatus;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.auth.service.AuthLocalService;
import com.moden.modenapi.modules.coupon.dto.CouponCreateFirstRegister;
import com.moden.modenapi.modules.coupon.dto.CouponCreateRequest;
import com.moden.modenapi.modules.coupon.dto.CouponFirstRegisterRes;
import com.moden.modenapi.modules.coupon.dto.CouponResponse;
import com.moden.modenapi.modules.coupon.service.CouponService;
import com.moden.modenapi.modules.coupon.service.CustomerCouponService;
import com.moden.modenapi.modules.customer.dto.CustomerListPageRes;
import com.moden.modenapi.modules.customer.dto.CustomerProfileUpdateReq;
import com.moden.modenapi.modules.customer.dto.CustomerResponse;
import com.moden.modenapi.modules.customer.dto.CustomerResponseForList;
import com.moden.modenapi.modules.customer.dto.CustomerSignUpRequest;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.reservation.repository.ReservationRepository;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService extends BaseService<CustomerDetail> {

    private final UserRepository userRepo;
    private final CustomerDetailRepository customerRepo;
    private final HairStudioDetailRepository studioRepo;
    private final AuthLocalService authLocalService;
    private final DesignerDetailRepository designerDetailRepository;
    private final CouponService couponService;
    private final CustomerCouponService customerCouponService;
    private final ReservationRepository reservationRepository;



    @Transactional
    public void customerRegister(CustomerSignUpRequest req, String rawPassword) {
        // 0) phone unique check
        userRepo.findByPhone(req.phone()).ifPresent(u -> {
            throw new IllegalArgumentException("User already registered with this phone number.");
        });

        // 1) Create User (CUSTOMER)
        User user = User.builder()
                .fullName(req.fullName())
                .phone(req.phone())
                .role(Role.CUSTOMER)
                .build();
        userRepo.save(user);
        authLocalService.createOrUpdatePassword(user.getId(), rawPassword);

        // 2) 현재 로그인한 actor (HAIR_STUDIO 또는 DESIGNER) 기준으로 studioId 구함
        UUID studioIdForActor = resolveStudioIdForCurrentActor(null);

        // 3) 기본 담당 디자이너 (옵션)
        UUID assignedDesignerId = resolveDesignerForAssignment(null, studioIdForActor);

        // 4) CustomerDetail 생성 (여기서 고객의 studioId 확정)
        CustomerDetail cd = CustomerDetail.builder()
                .userId(user.getId())
                .studioId(studioIdForActor)   // 고객이 속한 studio
                .designerId(assignedDesignerId)
                .email(null)
                .consentMarketing(false)
                .notificationEnabled(false)
                .build();
        customerRepo.save(cd);

        // 5) 첫 방문 쿠폰 생성 (CustomerDetail 기준)
        CouponFirstRegisterRes createdCoupon =
                couponService.createFirstVisitCouponForCustomer(cd);

        // 6) 고객에게 쿠폰 할당 (customer_coupon INSERT)
        customerCouponService.assignToCustomer(
                cd.getStudioId(),      // studioId
                createdCoupon.id(),    // coupon_id
                cd.getId()             // customer_id (CustomerDetail.id)
        );

        // 7) CustomerDetail 에 첫 방문 쿠폰 ID 저장
        cd.setFirstVisitCouponId(createdCoupon.id());
        customerRepo.save(cd);
    }




    /* ================= Helpers: context ================= */

    // HAIR_STUDIO: principal = STUDIO USER ID
// DESIGNER:    principal = DESIGNER USER ID -> DesignerDetail.hairStudioId = STUDIO USER ID
    private UUID resolveStudioIdForCurrentActor(UUID ignored) {
        if (hasRole("HAIR_STUDIO")) {
            // 🔹 토큰의 sub = 스튜디오 계정 userId 라고 가정
            UUID studioUserId = currentUserId();
            requireStudioByOwner(studioUserId);   // userId 기준으로 스튜디오 존재 확인
            return studioUserId;
        }

        if (hasRole("DESIGNER")) {
            UUID designerUserId = currentUserId();

            var dd = designerDetailRepository.findByUserIdAndDeletedAtIsNull(designerUserId)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.FORBIDDEN, "Designer profile not found")
                    );

            // 🔹 여기서도 hairStudioId 필드가 "스튜디오 userId" 라고 가정
            UUID studioUserId = dd.getHairStudioId();
            requireStudioByOwner(studioUserId);   // userId 기준으로 스튜디오 존재 확인
            return studioUserId;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Studio context is required");
    }
    private UUID resolveDesignerForAssignment(UUID ignored, UUID studioIdOfCustomer) {
        if (hasRole("DESIGNER")) {
            UUID me = currentUserId(); // ✅ designer userId

            var dd = designerDetailRepository.findByUserIdAndDeletedAtIsNull(me)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Designer profile not found"));

            if (!studioIdOfCustomer.equals(dd.getHairStudioId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Designer not in this studio");
            }

            // ✅ Endi CustomerDetail.designerId ga designer userId yozamiz
            return me;
        }
        // HAIR_STUDIO bo‘lsa — dizayner ixtiyoriy (null)
        return null;
    }


    /* -------------------- low-level helpers -------------------- */

    private UUID currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        String principal = (auth.getPrincipal() instanceof String s) ? s : auth.getName();
        try {
            return UUID.fromString(principal);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
        }
    }

    private boolean hasRole(String role) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        final String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(target::equals);
    }

    private HairStudioDetail requireStudioByOwner(UUID studioUserId) {
        return studioRepo.findByUserIdAndDeletedAtIsNull(studioUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Studio not found for current user"));
    }

    private void applyProfile(CustomerDetail detail, CustomerProfileUpdateReq req) {
        if (req.email() != null)            detail.setEmail(req.email());
        if (req.birthdate() != null)        detail.setBirthdate(req.birthdate());
        if (req.gender() != null)           detail.setGender(req.gender());
        if (req.address() != null)          detail.setAddress(req.address());
        if (req.consentMarketing() != null) detail.setConsentMarketing(req.consentMarketing());
        if (req.profileImageUrl() != null)  detail.setProfileImageUrl(req.profileImageUrl());
        if (req.visitReason() != null)      detail.setVisitReason(req.visitReason());
    }

    /* ================= CUSTOMER (self) ================= */

    @Transactional
    public CustomerDetail updateOwnProfile(CustomerProfileUpdateReq req) {
        UUID me = currentUserId();

        User u = userRepo.findActiveById(me)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found or deleted"));

        if (u.getRole() != Role.CUSTOMER && !hasRole("CUSTOMER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only CUSTOMER can update own profile");
        }

        CustomerDetail detail = customerRepo.findActiveByUserId(me)
                .orElseGet(() -> CustomerDetail.builder().userId(me).build());

        // keep existing studioId/designerId as-is
        applyProfile(detail, req);

        return customerRepo.save(detail);
    }

    /* ================= CUSTOMER (studio side) ================= */

    @Transactional
    public CustomerDetail updateCustomerAsStudio(UUID customerUserId, CustomerProfileUpdateReq req) {
        UUID studioUserId = currentUserId();
        HairStudioDetail studio = requireStudioByOwner(studioUserId);

        CustomerDetail target = customerRepo.findOneActiveInStudio(customerUserId, studio.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not in this studio"));

        applyProfile(target, req);
        return customerRepo.save(target);
    }

    @Transactional
    public void deleteCustomerAsStudio(UUID customerUserId) {
        UUID studioUserId = currentUserId();
        HairStudioDetail studio = requireStudioByOwner(studioUserId);

        CustomerDetail target = customerRepo.findOneActiveInStudio(customerUserId, studio.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not in this studio"));

        userRepo.findActiveById(customerUserId).ifPresent(u -> {
            u.setDeletedAt(Instant.now());
            userRepo.save(u);
        });
        target.setDeletedAt(Instant.now());
        customerRepo.save(target);
    }

    /**
     * 현재 로그인한 HAIR_STUDIO 사용자의 스튜디오에 속한 customer인지 검증.
     * 아니라면 403.
     */
    @Transactional(readOnly = true)
    public CustomerDetail ensureCustomerOfCurrentStudio(UUID customerUserId) {
        UUID ownerUserId = CurrentUserUtil.currentUserId();

        var studios = studioRepo.findActiveByUserIdOrderByUpdatedDesc(
                ownerUserId, PageRequest.of(0, 1)
        );
        if (studios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Studio not found for current user");
        }
        UUID studioId = studios.get(0).getId();

        return customerRepo
                .findByUserIdAndHairStudioIdAndDeletedAtIsNull(customerUserId, studioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Customer does not belong to your studio"
                ));
    }

    /* ================= CUSTOMER PROFILE (for studio) ================= */

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerProfileForStudio(UUID customerUserId) {
        // 1) 현재 스튜디오 소속인지 검증 + CustomerDetail 로드
        CustomerDetail c = ensureCustomerOfCurrentStudio(customerUserId);

        // 2) User 정보 로드
        User u = userRepo.findActiveById(customerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found or deleted"));

        // 3) DTO 매핑 (profile에서 필요한 필드들)
        return toCustomerProfileResponse(c, u);
    }

    /* ================= CUSTOMER LIST (for studio) ================= */

    @Transactional(readOnly = true)
    public CustomerListPageRes listStudioCustomers(
            String keyword,
            LocalDate fromDate,
            LocalDate toDate,
            int page,      // 1-based
            int limit      // rows per page
    ) {
        if (limit <= 0) limit = 10;
        if (page <= 0) page = 1;


        UUID currentUserId = CurrentUserUtil.currentUserId();

        // ✅ 비즈니스에서 쓰는 studioId = 항상 "studioUserId"
        UUID studioUserId;

        if (hasRole("HAIR_STUDIO")) {
            // 스튜디오 계정: 토큰의 sub = studioUserId
            // 필요하면 검증만 detail 테이블로
            studioRepo.findByUserIdAndDeletedAtIsNull(currentUserId)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.FORBIDDEN, "Studio not found for current user")
                    );
            studioUserId = currentUserId;

        } else if (hasRole("DESIGNER")) {
            // 디자이너 계정: DesignerDetail.hairStudioId = studioUserId
            var dd = designerDetailRepository.findByUserIdAndDeletedAtIsNull(currentUserId)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.FORBIDDEN, "Designer profile not found")
                    );

            studioUserId = dd.getHairStudioId(); // ❗ 여기 값도 userId 로 맞춰둔 상태여야 함

        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only studio or designer can list customers");
        }

        // ✅ 이제 CustomerDetail.studioId 도 studioUserId 로 저장돼 있다고 가정
        var customers = customerRepo.findAllByStudioId(studioUserId);
        if (customers.isEmpty()) {
            return new CustomerListPageRes(0, limit, page, List.of());
        }

        // 1) USER info (userId -> User)
        List<UUID> userIds = customers.stream()
                .map(CustomerDetail::getUserId)
                .filter(Objects::nonNull)
                .toList();

        Map<UUID, User> userMap = userRepo.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 2) lastVisitAt (customerId -> LocalDateTime)
        // Reservation.customerId = CustomerDetail.id 라고 가정
        List<UUID> customerIds = customers.stream()
                .map(CustomerDetail::getId)
                .toList();

        List<Object[]> lastVisitsRaw = reservationRepository.findLastVisitForCustomers(
                customerIds,
                ReservationStatus.COMPLETED
        );

        Map<UUID, LocalDateTime> lastVisitMap = lastVisitsRaw.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],          // customerId
                        row -> (LocalDateTime) row[1]  // max(reservationAt)
                ));

        // 3) filter + map → CustomerResponseForList
        List<CustomerResponseForList> all = customers.stream()
                .filter(c -> {
                    // keyword filter (name / phone)
                    if (keyword != null && !keyword.isBlank()) {
                        String k = keyword.toLowerCase();

                        User u = userMap.get(c.getUserId());
                        String name = Optional.ofNullable(u != null ? u.getFullName() : "")
                                .orElse("")
                                .toLowerCase();
                        String phone = Optional.ofNullable(u != null ? u.getPhone() : "")
                                .orElse("")
                                .toLowerCase();

                        if (!name.contains(k) && !phone.contains(k)) {
                            return false;
                        }
                    }

                    // date filter (createdAt)
                    if (fromDate != null || toDate != null) {
                        if (c.getCreatedAt() == null) return false;

                        LocalDate createdDate = c.getCreatedAt()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();

                        if (fromDate != null && createdDate.isBefore(fromDate)) return false;
                        if (toDate != null && createdDate.isAfter(toDate)) return false;
                    }

                    return true;
                })
                .map(c -> {
                    User u = userMap.get(c.getUserId());
                    LocalDateTime lastVisitAt = lastVisitMap.get(c.getId());
                    return toCustomerResponseForList(c, u, lastVisitAt);
                })
                .toList();

        int totalCount = all.size();

        // 4) pagination (in-memory slicing)
        int fromIndex = (page - 1) * limit;
        if (fromIndex >= totalCount) {
            return new CustomerListPageRes(totalCount, limit, page, List.of());
        }
        int toIndex = (int) Math.min(fromIndex + limit, totalCount);
        List<CustomerResponseForList> pageData = all.subList(fromIndex, toIndex);

        return new CustomerListPageRes(totalCount, limit, page, pageData);
    }

    private CustomerResponse toCustomerProfileResponse(CustomerDetail c, User u) {

        String designerName = null;

        if (c.getDesignerId() != null) {
            // ✅ endi c.getDesignerId() = designerUserId
            UUID designerUserId = c.getDesignerId();

            designerName = userRepo.findActiveById(designerUserId)
                    .map(User::getFullName)
                    .orElse(null);
        }

        return new CustomerResponse(
                c.getId(),
                u != null ? u.getFullName() : null,
                u != null ? u.getPhone() : null,
                u != null ? u.getRole() : null,
                c.getEmail(),
                c.getGender(),
                designerName,
                c.getBirthdate(),
                c.getAddress(),
                c.getProfileImageUrl(),
                c.getVisitReason(),
                c.isConsentMarketing()
        );
    }


    // 목록용 (리스트: id, fullName, phone, lastVisitAt)
    private CustomerResponseForList toCustomerResponseForList(
            CustomerDetail c,
            User u,
            LocalDateTime lastVisitAt
    ) {
        return new CustomerResponseForList(
                u != null ? u.getId() : null,
                u != null ? u.getFullName() : null,       // fullName
                u != null ? u.getPhone() : null,          // phone
                lastVisitAt                               // oxirgi tashrif
        );
    }

    /* ================= BaseService ================= */

    @Override
    protected JpaRepository<CustomerDetail, UUID> getRepository() {
        return customerRepo;
    }
}
