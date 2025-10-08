package com.moden.modenapi.modules.promo.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.moden.modenapi.modules.promo.model.CustomerCoupon;

public interface CustomerCouponRepository extends JpaRepository<CustomerCoupon, UUID> {}
