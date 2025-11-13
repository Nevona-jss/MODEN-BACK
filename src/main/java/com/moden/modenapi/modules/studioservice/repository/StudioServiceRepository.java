package com.moden.modenapi.modules.studioservice.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.studioservice.model.StudioService;

import java.util.List;
import java.util.UUID;

public interface StudioServiceRepository extends BaseRepository<StudioService, UUID> {
    List<StudioService> findByStudioId(UUID studioId);

}
