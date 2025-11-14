package com.moden.modenapi.modules.point.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.point.dto.StudioPointPolicyReq;
import com.moden.modenapi.modules.point.dto.StudioPointPolicyRes;
import com.moden.modenapi.modules.point.dto.StudioPointPolicyUpdateReq;
import com.moden.modenapi.modules.point.model.StudioPointPolicy;
import com.moden.modenapi.modules.point.repository.StudioPointPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StudioPointPolicyService extends BaseService<StudioPointPolicy> {

    private final StudioPointPolicyRepository policyRepository;

    // Default earn rate if policy not set (e.g. 0.0 or 5.0)
    @Value("${point.default-earn-rate:0.00}")
    private BigDecimal defaultRate;

    @Override
    protected StudioPointPolicyRepository getRepository() {
        return policyRepository;
    }

    /* Mapper */
    private StudioPointPolicyRes mapToRes(StudioPointPolicy p) {
        return new StudioPointPolicyRes(
                p.getId(),
                p.getStudioId(),
                p.getPointRate(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    /* Policy olish (studio bo‘yicha) */

    @Transactional(readOnly = true)
    public StudioPointPolicyRes getPolicyForStudio(UUID studioId) {
        StudioPointPolicy policy = policyRepository
                .findByStudioIdAndDeletedAtIsNull(studioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Point policy not set"));
        return mapToRes(policy);
    }

    public StudioPointPolicyRes upsertPolicy(UUID studioId, StudioPointPolicyReq req) {
        if (req.pointRate() == null || req.pointRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pointRate must be >= 0");
        }

        StudioPointPolicy policy = policyRepository
                .findByStudioIdAndDeletedAtIsNull(studioId)
                .orElse(StudioPointPolicy.builder()
                        .studioId(studioId)
                        .pointRate(req.pointRate())
                        .build()
                );

        policy.setPointRate(req.pointRate());
        policy = policy.getId() == null ? create(policy) : update(policy);

        return mapToRes(policy);
    }


    /**
     * Paymentda point hisoblash uchun ishlatish mumkin:
     * - Agar policy bo‘lsa, o‘sha
     * - Aks holda defaultRate qaytaradi
     */
    @Transactional(readOnly = true)
    public BigDecimal resolveRateForStudio(UUID studioId) {
        return policyRepository.findByStudioIdAndDeletedAtIsNull(studioId)
                .map(StudioPointPolicy::getPointRate)
                .orElse(defaultRate);
    }
}
