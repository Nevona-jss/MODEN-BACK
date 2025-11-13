package com.moden.modenapi.modules.coupon.repository;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.coupon.model.Coupon;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends BaseRepository<Coupon, UUID> {

    Optional<Coupon> findByIdAndDeletedAtIsNull(UUID id);

    List<Coupon> findAllByUserIdAndDeletedAtIsNull(UUID userId);

    List<Coupon> findAllByUserIdAndStatusAndDeletedAtIsNull(UUID userId, CouponStatus status);

    // ⬇️ Studio bo‘yicha qidirish (Entity qaytaradi)
    List<Coupon> findAllByStudioIdAndDeletedAtIsNull(UUID studioId);

    List<Coupon> findAllByStudioIdAndStatusAndDeletedAtIsNull(UUID studioId, CouponStatus status);
}
