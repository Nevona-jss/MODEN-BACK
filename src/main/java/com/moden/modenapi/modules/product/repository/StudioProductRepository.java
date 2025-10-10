package com.moden.modenapi.modules.product.repository;

import com.moden.modenapi.modules.product.model.StudioProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface StudioProductRepository extends JpaRepository<StudioProduct, UUID> {}
