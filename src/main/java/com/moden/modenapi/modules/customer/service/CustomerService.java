package com.moden.modenapi.modules.customer.service;

import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.customer.dto.CustomerProfileUpdateReq;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import com.moden.modenapi.modules.designer.model.DesignerDetail;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService extends BaseService<CustomerDetail> {

    private final UserRepository userRepo;
    private final CustomerDetailRepository customerRepo;
    private final HairStudioDetailRepository studioRepo;
    private final DesignerDetailRepository designerRepo;

    /* -------------------- helpers -------------------- */

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

    private DesignerDetail requireDesignerByUser(UUID designerUserId) {
        return designerRepo.findByUserIdAndDeletedAtIsNull(designerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Designer not found for current user"));
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

    /* -------------------- STUDIO actions -------------------- */

    @Transactional(readOnly = true)
    public List<CustomerDetail> listStudioCustomers() {
        UUID studioUserId = currentUserId();
        HairStudioDetail studio = requireStudioByOwner(studioUserId);
        return customerRepo.findAllActiveByStudio(studio.getId());
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

    /* -------------------- DESIGNER actions -------------------- */

    @Transactional(readOnly = true)
    public List<CustomerDetail> listDesignerCustomers() {
        UUID designerUserId = currentUserId();
        DesignerDetail d = requireDesignerByUser(designerUserId);
        return customerRepo.findAllActiveForDesigner(d.getId(), designerUserId);
    }

    @Transactional
    public CustomerDetail updateCustomerAsDesigner(UUID customerUserId, CustomerProfileUpdateReq req) {
        UUID designerUserId = currentUserId();
        DesignerDetail d = requireDesignerByUser(designerUserId);

        CustomerDetail target = customerRepo.findOneActiveForDesigner(customerUserId, d.getId(), designerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not assigned to this designer"));

        applyProfile(target, req);
        return customerRepo.save(target);
    }

    @Transactional
    public void deleteCustomerAsDesigner(UUID customerUserId) {
        UUID designerUserId = currentUserId();
        DesignerDetail d = requireDesignerByUser(designerUserId);

        CustomerDetail target = customerRepo.findOneActiveForDesigner(customerUserId, d.getId(), designerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not assigned to this designer"));

        userRepo.findActiveById(customerUserId).ifPresent(u -> {
            u.setDeletedAt(Instant.now());
            userRepo.save(u);
        });
        target.setDeletedAt(Instant.now());
        customerRepo.save(target);
    }

    @Override
    protected JpaRepository<CustomerDetail, UUID> getRepository() {
        return customerRepo;
    }
}
