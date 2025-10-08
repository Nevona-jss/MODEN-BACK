package com.moden.modenapi.modules.promo.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.moden.modenapi.modules.promo.model.Coupon;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {}
