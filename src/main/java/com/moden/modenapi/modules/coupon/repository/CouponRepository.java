package com.moden.modenapi.modules.coupon.repository;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.coupon.model.Coupon;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends BaseRepository<Coupon, UUID> {

    Optional<Coupon> findByIdAndDeletedAtIsNull(UUID id);

    List<Coupon> findAllByUserIdAndDeletedAtIsNull(UUID userId);

    List<Coupon> findAllByUserIdAndStatusAndDeletedAtIsNull(UUID userId, CouponStatus status);

    List<Coupon> findAllByDeletedAtIsNull();

}
