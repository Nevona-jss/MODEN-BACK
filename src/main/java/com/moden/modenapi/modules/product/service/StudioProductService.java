package com.moden.modenapi.modules.product.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.product.model.StudioProduct;
import com.moden.modenapi.modules.product.repository.StudioProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * Handles salon and marketplace products.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class StudioProductService extends BaseService<StudioProduct, UUID> {

    private final StudioProductRepository repo;

    @Override
    protected JpaRepository<StudioProduct, UUID> getRepository() {
        return repo;
    }

    /**
     * Updates stock quantity for a product.
     *
     * @param productId UUID of the product
     * @param newStock new quantity value
     * @return updated product
     */
    public StudioProduct updateStock(UUID productId, int newStock) {
        var product = getById(productId);
        product.setStock(newStock);
        return save(product);
    }
}
