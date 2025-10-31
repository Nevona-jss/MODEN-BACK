package com.moden.modenapi.modules.studio.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;

import java.util.Optional;
import java.util.UUID;

public interface HairStudioDetailRepository extends BaseRepository<HairStudioDetail, UUID> {

    // 🔹 find by studio login code
    Optional<HairStudioDetail> findByIdForLogin(String idForLogin);

    // 🔹 find by owning user (what you asked for)
    Optional<HairStudioDetail> findByUserId(UUID userId);
    Optional<HairStudioDetail> findByUserIdAndDeletedAtIsNull(UUID userId);
}
