package com.moden.modenapi.modules.studioservice.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.studioservice.model.StudioService;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StudioServiceRepository extends BaseRepository<StudioService, UUID> {

    List<StudioService> findByStudioId(UUID studioId);

    @Query("""
        select s
        from StudioService s
        where s.studioId = :studioId
          and s.id in :serviceIds
    """)
    List<StudioService> findAllByStudioAndIds(
            @Param("studioId") UUID studioId,
            @Param("serviceIds") List<UUID> serviceIds
    );
}
