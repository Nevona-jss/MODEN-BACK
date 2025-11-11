package com.moden.modenapi.modules.designer.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.designer.model.DesignerDetail;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DesignerDetailRepository extends BaseRepository<DesignerDetail, UUID> {
    Optional<DesignerDetail> findByUserIdAndDeletedAtIsNull(UUID userId);
    Optional<DesignerDetail> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByIdForLogin(String idForLogin);

    Optional<DesignerDetail> findByIdForLoginIgnoreCase(String idForLogin);

    Optional<DesignerDetail> findByUserId(UUID userId);

    Optional<DesignerDetail> findActiveById(UUID id);
    @Query("""
        select d
        from DesignerDetail d
        where d.userId = :userId and d.deletedAt is null
        order by coalesce(d.updatedAt, d.createdAt) desc
    """)
    List<DesignerDetail> findActiveByUserIdOrderByUpdatedDesc(@Param("userId") UUID userId, Pageable pageable);


}
