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
          and s.afterService like concat('%', :keyword, '%')
    """)
    List<StudioService> searchByStudioIdAndAfterService(
            @Param("studioId") UUID studioId,
            @Param("keyword") String keyword
    );
}
