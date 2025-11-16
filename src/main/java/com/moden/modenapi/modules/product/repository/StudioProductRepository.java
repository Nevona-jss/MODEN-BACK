package com.moden.modenapi.modules.product.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.product.model.StudioProduct;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudioProductRepository extends BaseRepository<StudioProduct, UUID> {

    List<StudioProduct> findByStudioId(UUID studioId);
    List<StudioProduct> findAllByStudioIdAndDeletedAtIsNullOrderByProductNameAsc(UUID studioId);
    // 단건(삭제되지 않은 것만)
    Optional<StudioProduct> findByIdAndDeletedAtIsNull(UUID id);

    // 스튜디오별 목록(삭제되지 않은 것만, 최신순)
    List<StudioProduct> findAllByStudioIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID studioId);

    // BaseService에서 호출하던 이름을 그대로 맞추기 위한 alias
    default Optional<StudioProduct> findActiveById(UUID id) {
        return findByIdAndDeletedAtIsNull(id);
    }

    // 상품명에서 검색
    List<StudioProduct> findByStudioIdAndProductNameContainingIgnoreCase(
            UUID studioId,
            String keyword
    );

    // 비고(notes)에서 검색
    List<StudioProduct> findByStudioIdAndNotesContainingIgnoreCase(
            UUID studioId,
            String keyword
    );

}
