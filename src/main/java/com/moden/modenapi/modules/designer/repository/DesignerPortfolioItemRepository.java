package com.moden.modenapi.modules.designer.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.designer.model.DesignerPortfolioItem;

import java.util.List;
import java.util.UUID;

public interface DesignerPortfolioItemRepository
        extends BaseRepository<DesignerPortfolioItem, UUID> {

    /** Get by ids (order not guaranteed; keep order in service if needed) */
    List<DesignerPortfolioItem> findAllByIdIn(List<UUID> ids);

    /** Active (not soft-deleted) items of a designer, oldest → newest */
    List<DesignerPortfolioItem> findAllByDesignerIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID designerId);

}
