package com.moden.modenapi.modules.point.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.point.dto.StudioPointPolicyReq;
import com.moden.modenapi.modules.point.dto.StudioPointPolicyRes;
import com.moden.modenapi.modules.point.model.StudioPointPolicy;
import com.moden.modenapi.modules.point.repository.StudioPointPolicyRepository;
import com.moden.modenapi.modules.studioservice.model.StudioService;
import com.moden.modenapi.modules.studioservice.repository.StudioServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StudioPointPolicyService extends BaseService<StudioPointPolicy> {

    private final StudioPointPolicyRepository repository;
    private final StudioServiceRepository studioServiceRepository;

    @Override
    protected StudioPointPolicyRepository getRepository() {
        return repository;
    }

    // ----------------------------------------------------------------------
    // 🔹 Create or update studio’s point policy
    // ----------------------------------------------------------------------
    public StudioPointPolicyRes setPolicy(UUID studioId, StudioPointPolicyReq req) {
        StudioPointPolicy policy = repository.findByStudioIdAndDeletedAtIsNull(studioId)
                .orElse(StudioPointPolicy.builder()
                        .studioId(studioId)
                        .build());

        policy.setPointRate(req.pointRate());
        policy.setUpdatedAt(Instant.now());
        repository.save(policy);

        return toRes(policy);
    }

    // ----------------------------------------------------------------------
    // 🔹 Get policy by studio ID (returns DTO)
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public StudioPointPolicyRes getPolicy(UUID studioId) {
        StudioPointPolicy policy = repository.findByStudioIdAndDeletedAtIsNull(studioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Studio point policy not found"));
        return toRes(policy);
    }

    // ----------------------------------------------------------------------
    // 🔹 Get policy by service ID (returns ENTITY)
    // ----------------------------------------------------------------------
//    @Transactional(readOnly = true)
//    public StudioPointPolicy getPolicyByService(UUID serviceId) {
//        StudioService service = studioServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
//
//        return repository.findByStudioIdAndDeletedAtIsNull(service.getStudioId())
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Point policy not found"));
//    }

    // ----------------------------------------------------------------------
    // 🔹 Mapper
    // ----------------------------------------------------------------------
    private StudioPointPolicyRes toRes(StudioPointPolicy policy) {
        return new StudioPointPolicyRes(
                policy.getId(),
                policy.getStudioId(),
                policy.getPointRate(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
