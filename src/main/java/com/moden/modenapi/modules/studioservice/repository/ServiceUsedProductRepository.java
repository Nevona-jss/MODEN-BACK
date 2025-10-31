package com.moden.modenapi.modules.studioservice.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.studioservice.model.ServiceUsedProduct;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface ServiceUsedProductRepository extends BaseRepository<ServiceUsedProduct, UUID> {

    // 🔹 Xizmatdagi barcha mahsulotlar
    @Query("SELECT p FROM ServiceUsedProduct p WHERE p.serviceId = :serviceId")
    List<ServiceUsedProduct> findAllByServiceId(UUID serviceId);

    // 🔹 Xizmatni yangilaganda eski mahsulotlarni o‘chirish uchun
    void deleteAllByServiceId(UUID serviceId);
}
