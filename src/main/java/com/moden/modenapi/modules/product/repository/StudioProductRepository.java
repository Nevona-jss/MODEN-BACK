package com.moden.modenapi.modules.product.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.product.model.StudioProduct;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudioProductRepository extends BaseRepository<StudioProduct, UUID> {

    // 🔹 Faqat o‘chirilmagan mahsulotlar (deletedAt IS NULL)
    @Query("SELECT p FROM StudioProduct p WHERE p.studioId = :studioId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<StudioProduct> findAllActiveByStudioId(UUID studioId);

    // 🔹 Faqat faol (o‘chirilmagan) mahsulot
    @Query("SELECT p FROM StudioProduct p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<StudioProduct> findActiveById(UUID id);
}
