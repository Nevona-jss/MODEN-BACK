package com.moden.modenapi.modules.billing.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.moden.modenapi.modules.billing.model.Sale;

public interface SaleRepository extends JpaRepository<Sale, UUID> {}
