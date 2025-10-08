package com.moden.modenapi.modules.promo.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.moden.modenapi.modules.promo.model.PointLedger;

public interface PointLedgerRepository extends JpaRepository<PointLedger, UUID> {}
