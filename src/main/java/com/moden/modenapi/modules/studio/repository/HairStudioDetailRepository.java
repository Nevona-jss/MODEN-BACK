package com.moden.modenapi.modules.studio.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.designer.model.DesignerPortfolioItem;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HairStudioDetailRepository extends BaseRepository<HairStudioDetail, UUID> {

    // Login code existence (case-insensitive helper too)
    boolean existsByIdForLogin(String idForLogin);
    boolean existsByIdForLoginIgnoreCase(String idForLogin);

    @Query("""
        select s
        from HairStudioDetail s
        where s.userId = :userId and s.deletedAt is null
        order by coalesce(s.updatedAt, s.createdAt) desc
    """)
    List<HairStudioDetail> findActiveByUserIdOrderByUpdatedDesc(@Param("userId") UUID userId, Pageable pageable);
    // 🔹 Inspect possible duplicates for a user (active only)
    List<HairStudioDetail> findAllByUserIdAndDeletedAtIsNull(UUID userId);

    // 🔹 Lookup by studio login code
    Optional<HairStudioDetail> findByIdForLogin(String idForLogin);
    Optional<HairStudioDetail> findByIdForLoginIgnoreCase(String idForLogin);

    // 🔹 Lookup by owning user (any / active-only)
    Optional<HairStudioDetail> findByUserId(UUID userId);
    Optional<HairStudioDetail> findByUserIdAndDeletedAtIsNull(UUID userId);
}
