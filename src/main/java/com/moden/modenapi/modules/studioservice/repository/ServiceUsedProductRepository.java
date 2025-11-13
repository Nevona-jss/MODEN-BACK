package com.moden.modenapi.modules.studioservice.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.studioservice.model.ServiceUsedProduct;

import java.util.List;
import java.util.UUID;

public interface ServiceUsedProductRepository extends BaseRepository<ServiceUsedProduct, UUID> {
    List<ServiceUsedProduct> findByServiceId(UUID serviceId);

    void deleteByServiceId(UUID serviceId);

}
