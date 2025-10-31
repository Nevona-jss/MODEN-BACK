package com.moden.modenapi.modules.designer.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.designer.model.DesignerDetail;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DesignerDetailRepository extends BaseRepository<DesignerDetail, UUID> {
    boolean existsByIdForLogin(String idForLogin);
    Optional<DesignerDetail> findByUserId(UUID userId);
    Optional<DesignerDetail> findActiveById(UUID id);
    List<DesignerDetail> findAllActiveByHairStudioId(UUID studioId);
}
