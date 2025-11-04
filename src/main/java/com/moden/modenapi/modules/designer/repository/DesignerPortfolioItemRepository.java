package com.moden.modenapi.modules.designer.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.designer.model.DesignerPortfolioItem;
import java.util.List;
import java.util.UUID;

public interface DesignerPortfolioItemRepository
        extends BaseRepository<DesignerPortfolioItem, UUID> {

    // ID ro‘yxati bilan olish (tartibni xizmat qismida siz saqlaysiz)
    List<DesignerPortfolioItem> findAllByIdIn(List<UUID> ids);


}
