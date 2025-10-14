package com.moden.modenapi.modules.studio.service;

import com.moden.modenapi.common.enums.UserType;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.studio.dto.StudioCreateReq;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.model.HairStudio;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * ✅ HairStudioService
 * Handles CRUD and automatic owner registration.
 */
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
     * 🔹 Create new Hair Studio and its owner.
     */
    public StudioRes create(StudioCreateReq req) {

        // 1️⃣ Check or create owner user
        User ownerUser = userRepository.findByPhone(req.ownerPhone())
                .orElseGet(() -> {
                    User newOwner = User.builder()
                            .name(req.owner())
                            .phone(req.ownerPhone())
                            .userType(UserType.HAIR_STUDIO)
                            .build();
                    return userRepository.save(newOwner);
                });

        // 2️⃣ Create hair studio entity
        HairStudio studio = HairStudio.builder()
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

        // 3️⃣ Optional detail entity
        HairStudioDetail detail = HairStudioDetail.builder()
                .studio(studio)
                .build();
        studio.setDetail(detail);

        // 4️⃣ Save studio
        HairStudio saved = save(studio);

        // 5️⃣ Return DTO
        return new StudioRes(
                saved.getId(),
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
     * 🔹 Retrieve all studios
     */
    @Transactional(readOnly = true)
    public List<StudioRes> list() {
        return getAll().stream()
                .map(s -> new StudioRes(
                        s.getId(),
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

    /**
     * 🔹 Retrieve one studio by ID
     */
    @Transactional(readOnly = true)
    public StudioRes get(UUID id) {
        HairStudio s = getById(id);
        return new StudioRes(
                s.getId(),
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
}
