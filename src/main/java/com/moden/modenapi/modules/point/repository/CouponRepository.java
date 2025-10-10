package com.moden.modenapi.modules.point.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.moden.modenapi.modules.point.model.Coupon;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {}
