package com.moden.modenapi.modules.studioservice.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.studioservice.dto.StudioServiceCreateRequest;
import com.moden.modenapi.modules.studioservice.dto.StudioServiceRes;
import com.moden.modenapi.modules.studioservice.dto.StudioServiceUpdateReq;
import com.moden.modenapi.modules.studioservice.model.StudioService;
import com.moden.modenapi.modules.studioservice.repository.StudioServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class StudioServiceService extends BaseService<StudioService> {

    private final StudioServiceRepository studioServiceRepository;

    // CREATE
    public StudioServiceRes create(UUID studioId, StudioServiceCreateRequest req) {
        StudioService entity = StudioService.builder()
                .studioId(studioId)
                .serviceName(req.serviceName())
                .afterService(req.afterService())
                .durationMin(req.durationMin())
                .servicePrice(req.servicePrice())
                .designerTipPercent(req.designerTipPercent())
                .build();

        StudioService saved = studioServiceRepository.save(entity);
        return toRes(saved);
    }

    // UPDATE
    public StudioServiceRes update(UUID studioId, UUID serviceId, StudioServiceUpdateReq req) {
        StudioService entity = studioServiceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("StudioService not found: " + serviceId));

        if (!entity.getStudioId().equals(studioId)) {
            throw new IllegalArgumentException("You do not have permission to update this service.");
        }

        entity.setServiceName(req.serviceName());
        entity.setAfterService(req.afterService());
        entity.setDurationMin(req.durationMin());
        entity.setServicePrice(req.servicePrice());
        entity.setDesignerTipPercent(req.designerTipPercent());

        return toRes(entity);
    }

    // DELETE
    public void delete(UUID studioId, UUID serviceId) {
        StudioService entity = studioServiceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("StudioService not found: " + serviceId));

        if (!entity.getStudioId().equals(studioId)) {
            throw new IllegalArgumentException("You do not have permission to delete this service.");
        }

        studioServiceRepository.delete(entity);
    }

    // DETAIL
    @Transactional(readOnly = true)
    public StudioServiceRes getOne(UUID studioId, UUID serviceId) {
        StudioService entity = studioServiceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("StudioService not found: " + serviceId));

        if (!entity.getStudioId().equals(studioId)) {
            throw new IllegalArgumentException("You do not have permission to view this service.");
        }

        return toRes(entity);
    }

    // LIST
    @Transactional(readOnly = true)
    public List<StudioServiceRes> listByStudio(UUID studioId) {
        List<StudioService> services = studioServiceRepository.findByStudioId(studioId);
        return services.stream()
                .map(this::toRes)
                .toList();
    }

    // ---- private helpers ----

    private StudioServiceRes toRes(StudioService service) {
        return new StudioServiceRes(
                service.getId(),
                service.getServiceName(),
                service.getAfterService(),
                service.getDurationMin(),
                service.getServicePrice(),
                service.getDesignerTipPercent(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }

    @Override
    protected JpaRepository<StudioService, UUID> getRepository() {
        return studioServiceRepository;
    }
}
