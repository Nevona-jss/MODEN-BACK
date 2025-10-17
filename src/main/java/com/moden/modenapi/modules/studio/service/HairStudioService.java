package com.moden.modenapi.modules.studio.service;

import com.moden.modenapi.common.enums.UserType;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.common.utils.StudioIdGenerator;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.studio.dto.StudioCreateReq;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.dto.StudioUpdateReq;
import com.moden.modenapi.modules.studio.model.HairStudio;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class HairStudioService extends BaseService<HairStudio, UUID> {

    private final HairStudioRepository repo;
    private final UserRepository userRepository;

    @Override
    protected JpaRepository<HairStudio, UUID> getRepository() {
        return repo;
    }

    /**
     * 🔹 Create new Hair Studio + owner auto registration (ownerPhone is optional)
     */
    public StudioRes create(StudioCreateReq req) {

        // 1️⃣ Register or find owner (only if ownerPhone provided)
        if (req.ownerPhone() != null && !req.ownerPhone().isBlank()) {
            userRepository.findByPhone(req.ownerPhone())
                    .orElseGet(() -> {
                        User newOwner = User.builder()
                                .name(req.owner())
                                .phone(req.ownerPhone())
                                .userType(UserType.HAIR_STUDIO)
                                .build();
                        return userRepository.save(newOwner);
                    });
        }

        // 2️⃣ Build HairStudio
        HairStudio studio = HairStudio.builder()
                .idForLogin(StudioIdGenerator.generateId(req.name()))
                .name(req.name())
                .businessNo(req.businessNo())
                .owner(req.owner())
                .ownerPhone(req.ownerPhone())
                .studioPhone(req.studioPhone())
                .address(req.address())
                .logo(req.logo())
                .instagram(req.instagram())
                .naver(req.naver())
                .build();

        // 3️⃣ Create detail
        HairStudioDetail detail = HairStudioDetail.builder()
                .studio(studio)
                .build();
        studio.setDetail(detail);

        // 4️⃣ Save
        HairStudio saved = save(studio);

        // 5️⃣ Return DTO
        return new StudioRes(
                saved.getId(),
                saved.getIdForLogin(),
                saved.getName(),
                saved.getBusinessNo(),
                saved.getOwner(),
                saved.getOwnerPhone(),
                saved.getStudioPhone(),
                saved.getAddress(),
                saved.getLogo(),
                saved.getInstagram(),
                saved.getNaver()
        );
    }

    @Transactional(readOnly = true)
    public List<StudioRes> list() {
        return getAll().stream()
                .map(s -> new StudioRes(
                        s.getId(),
                        s.getIdForLogin(),
                        s.getName(),
                        s.getBusinessNo(),
                        s.getOwner(),
                        s.getOwnerPhone(),
                        s.getStudioPhone(),
                        s.getAddress(),
                        s.getLogo(),
                        s.getInstagram(),
                        s.getNaver()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudioRes get(String idForLogin) {
        HairStudio s = repo.findByIdForLogin(idForLogin)
                .orElseThrow(() -> new IllegalArgumentException("Studio not found: " + idForLogin));
        return new StudioRes(
                s.getId(),
                s.getIdForLogin(),
                s.getName(),
                s.getBusinessNo(),
                s.getOwner(),
                s.getOwnerPhone(),
                s.getStudioPhone(),
                s.getAddress(),
                s.getLogo(),
                s.getInstagram(),
                s.getNaver()
        );
    }

    /**
     * Update studio profile (only ADMIN or owning HAIR_STUDIO allowed).
     * Authentication passed from controller.
     */
    public StudioRes updateProfile(UUID id, StudioUpdateReq req, Authentication auth) {
        HairStudio s = getById(id); // throws if not found

        // check authority
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ADMIN".equals(a.getAuthority()));

        if (!isAdmin) {
            // resolve caller phone: try parse auth name as UUID -> load user -> get phone
            String callerPhone = resolveCallerPhone(auth);
            String studioOwnerPhone = Optional.ofNullable(s.getOwnerPhone()).orElse("");

            if (!studioOwnerPhone.equals(callerPhone)) {
                throw new AccessDeniedException("You are not allowed to update this studio profile");
            }
        }

        // apply updates (only when not null)
        if (req.ownerPhone() != null) s.setOwnerPhone(req.ownerPhone());
        if (req.studioPhone() != null) s.setStudioPhone(req.studioPhone());
        if (req.address() != null) s.setAddress(req.address());
        if (req.logo() != null) s.setLogo(req.logo());
        if (req.instagram() != null) s.setInstagram(req.instagram());
        if (req.naver() != null) s.setNaver(req.naver());

        HairStudio saved = save(s);

        return new StudioRes(
                saved.getId(),
                saved.getIdForLogin(),
                saved.getName(),
                saved.getBusinessNo(),
                saved.getOwner(),
                saved.getOwnerPhone(),
                saved.getStudioPhone(),
                saved.getAddress(),
                saved.getLogo(),
                saved.getInstagram(),
                saved.getNaver()
        );
    }

    /**
     * Helper: derive caller's phone from Authentication.
     * - If auth.getName() is a UUID, try to load User by id and return phone.
     * - Otherwise assume auth.getName() is the phone.
     *
     * Adjust this if your JWT principal contains a different value.
     */
    private String resolveCallerPhone(Authentication auth) {
        String name = auth.getName();
        try {
            UUID uid = UUID.fromString(name);
            return userRepository.findById(uid)
                    .map(User::getPhone)
                    .orElse(name);
        } catch (IllegalArgumentException ex) {
            // not a UUID -> treat as phone (common)
            return name;
        }
    }
}
