package com.moden.modenapi.modules.customer.service;

import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.auth.service.AuthLocalService;
import com.moden.modenapi.modules.coupon.dto.CouponCreateRequest;
import com.moden.modenapi.modules.coupon.dto.CouponResponse;
import com.moden.modenapi.modules.coupon.service.CouponService;
import com.moden.modenapi.modules.coupon.service.CustomerCouponService;
import com.moden.modenapi.modules.customer.dto.CustomerProfileUpdateReq;
import com.moden.modenapi.modules.customer.dto.CustomerSignUpRequest;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.designer.service.DesignerService;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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




    @Transactional
    public void customerRegister(CustomerSignUpRequest req, String rawPassword) {
        // 0) phone unique check
        userRepo.findByPhone(req.phone()).ifPresent(u -> {
            throw new IllegalArgumentException("User already registered with this phone number.");
        });

        // 1) Create User
        User user = User.builder()
                .fullName(req.fullName())
                .phone(req.phone())
                .role(Role.CUSTOMER)
                .build();
        userRepo.save(user);
        authLocalService.createOrUpdatePassword(user.getId(), rawPassword);

        // 2) Resolve studio for current actor (studioId from logged-in studio)
        UUID studioId = resolveStudioIdForCurrentActor(null);

        // 3) Resolve designer (optional)
        UUID assignedDesignerId = resolveDesignerForAssignment(null, studioId);

        // 4) Create CustomerDetail
        CustomerDetail cd = CustomerDetail.builder()
                .userId(user.getId())
                .studioId(studioId)
                .designerId(assignedDesignerId)
                .email(null)
                .consentMarketing(false)
                .notificationEnabled(false)
                .build();
        customerRepo.save(cd);

        // 5) First-visit coupon (personal, isGlobal = false)
        CouponCreateRequest firstVisitReq = new CouponCreateRequest(
                studioId,                      // studioId
                user.getId(),                  // userId (policy side, as you designed)
                "💈 First Visit — 10% discount",
                BigDecimal.valueOf(10.0),      // discountRate
                null,                          // discountAmount
                LocalDate.now(),               // startDate = today
                LocalDate.now().plusDays(30),  // expiryDate = 30 days
                false,                         // birthdayCoupon
                true,                          // firstVisitCoupon
                false                          // isGlobal = false  ✅ personal coupon
        );

        // Create coupon policy
        CouponResponse createdCoupon = couponService.create(firstVisitReq);

        // 6) Insert into customer_coupon (status = AVAILABLE)
        //    customerId = CustomerDetail.id (business customer)
        customerCouponService.assignToCustomer(
                studioId,
                createdCoupon.id(),   // coupon_id
                cd.getId()            // customer_id
        );

        // 7) (optional) also store coupon id in CustomerDetail
        cd.setFirstVisitCouponId(createdCoupon.id());
        customerRepo.save(cd);
    }


    /* ================= Helpers ================= */

    // HAIR_STUDIO: principal = STUDIO ID
// DESIGNER:    principal = DESIGNER USER ID -> DesignerDetail.hairStudioId
    private UUID resolveStudioIdForCurrentActor(UUID ignored) {
        if (hasRole("HAIR_STUDIO")) {
            UUID studioId = currentStudioId(); // sizda principal studio UUID
            requireStudio(studioId);
            return studioId;
        }
        if (hasRole("DESIGNER")) {
            UUID designerUserId = currentUserId();
            var dd = designerDetailRepository.findByUserIdAndDeletedAtIsNull(designerUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Designer profile not found"));
            UUID studioId = dd.getHairStudioId();
            requireStudio(studioId);
            return studioId;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Studio context is required");
    }

    // DESIGNER aktor bo‘lsa va designerId kiritilmagan bo‘lsa — o‘zini tayinlash.
// Aks holda (HAIR_STUDIO aktor), dizayner tayinlanmasligi mumkin (null).
    private UUID resolveDesignerForAssignment(UUID ignored, UUID studioIdOfCustomer) {
        if (hasRole("DESIGNER")) {
            UUID me = currentUserId(); // userId
            var dd = designerDetailRepository.findByUserIdAndDeletedAtIsNull(me)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Designer profile not found"));
            if (!studioIdOfCustomer.equals(dd.getHairStudioId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Designer not in this studio");
            }
            return dd.getId(); // CustomerDetail.designerId = DesignerDetail.id
        }
        // HAIR_STUDIO bo‘lsa — dizayner ixtiyoriy (null)
        return null;
    }



    /* -------------------- helpers -------------------- */

    private UUID currentStudioId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        String principal = (auth.getPrincipal() instanceof String s) ? s : auth.getName();
        try { return UUID.fromString(principal); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal (expected STUDIO ID)"); }
    }

    private HairStudioDetail requireStudio(UUID studioId) {
        return studioRepo.findByIdAndDeletedAtIsNull(studioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Studio not found or deleted"));
    }


    private UUID currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        String principal = (auth.getPrincipal() instanceof String s) ? s : auth.getName();
        try { return UUID.fromString(principal); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal"); }
    }

    private boolean hasRole(String role) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        final String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(target::equals);
    }

    private HairStudioDetail requireStudioByOwner(UUID studioUserId) {
        return studioRepo.findByUserIdAndDeletedAtIsNull(studioUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Studio not found for current user"));
    }

    private void applyProfile(CustomerDetail detail, CustomerProfileUpdateReq req) {
        if (req.email() != null)            detail.setEmail(req.email());
        if (req.birthdate() != null)        detail.setBirthdate(req.birthdate());
        if (req.gender() != null)           detail.setGender(req.gender()); // ✅ direct enum
        if (req.address() != null)          detail.setAddress(req.address());
        if (req.consentMarketing() != null) detail.setConsentMarketing(req.consentMarketing());
        if (req.profileImageUrl() != null)  detail.setProfileImageUrl(req.profileImageUrl());
        if (req.visitReason() != null)      detail.setVisitReason(req.visitReason());
    }


    /* -------------------- CUSTOMER (self) -------------------- */

    @Transactional
    public CustomerDetail updateOwnProfile(CustomerProfileUpdateReq req) {
        UUID me = currentUserId();

        User u = userRepo.findActiveById(me)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found or deleted"));

        if (u.getRole() != Role.CUSTOMER && !hasRole("CUSTOMER"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only CUSTOMER can update own profile");

        CustomerDetail detail = customerRepo.findActiveByUserId(me)
                .orElseGet(() -> CustomerDetail.builder().userId(me).build());

        // keep existing studioId/designerId as-is
        applyProfile(detail, req);

        return customerRepo.save(detail);
    }

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

    @Transactional(readOnly = true)
    public List<CustomerDetail> listStudioCustomers() {
        UUID ownerUserId = CurrentUserUtil.currentUserId();

        var studios = studioRepo.findActiveByUserIdOrderByUpdatedDesc(
                ownerUserId, PageRequest.of(0, 1)
        );
        if (studios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Studio not found for current user");
        }
        UUID studioId = studios.get(0).getId();

        return customerRepo.findAllByStudioId(studioId);
    }


    @Override
    protected JpaRepository<CustomerDetail, UUID> getRepository() {
        return customerRepo;
    }
}
