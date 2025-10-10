package com.moden.modenapi.modules.studio.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.studio.dto.StudioCreateReq;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.model.HairStudio;
import com.moden.modenapi.modules.studio.repository.HairStudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service layer that manages Hair Studio CRUD operations.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class HairStudioService extends BaseService<HairStudio, UUID> {

    private final HairStudioRepository repo;

    @Override
    protected JpaRepository<HairStudio, UUID> getRepository() {
        return repo;
    }

    /**
     * 🔹 Creates a new hair studio record.
     */
    public StudioRes create(StudioCreateReq req) {
        var entity = HairStudio.builder()
                .name(req.name())
                .qrCodeUrl(req.qrCodeUrl())
                .businessNo(req.businessNo())
                .address(req.address())
                .phone(req.phone())
                .build();

        var saved = save(entity);

        return new StudioRes(
                saved.getId(),
                saved.getName(),
                saved.getQrCodeUrl(),
                saved.getBusinessNo(),
                saved.getAddress(),
                saved.getPhone()
        );
    }

    /**
     * 🔹 Retrieves all studios as DTO list.
     */
    @Transactional(readOnly = true)
    public List<StudioRes> list() {
        return getAll().stream()
                .map(s -> new StudioRes(
                        s.getId(),
                        s.getName(),
                        s.getQrCodeUrl(),
                        s.getBusinessNo(),
                        s.getAddress(),
                        s.getPhone()
                ))
                .toList();
    }

    /**
     * 🔹 Retrieves a single studio by ID and maps it to a DTO.
     */
    @Transactional(readOnly = true)
    public StudioRes get(UUID id) {
        var s = getById(id); // ✅ BaseService already provides getById(ID)
        return new StudioRes(
                s.getId(),
                s.getName(),
                s.getQrCodeUrl(),
                s.getBusinessNo(),
                s.getAddress(),
                s.getPhone()
        );
    }
}
