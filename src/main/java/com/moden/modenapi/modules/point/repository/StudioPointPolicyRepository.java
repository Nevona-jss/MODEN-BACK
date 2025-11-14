package com.moden.modenapi.modules.point.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.point.model.StudioPointPolicy;
import java.util.Optional;
import java.util.UUID;

public interface StudioPointPolicyRepository extends BaseRepository<StudioPointPolicy, UUID> {
    Optional<StudioPointPolicy> findByStudioIdAndDeletedAtIsNull(UUID studioId);

}
