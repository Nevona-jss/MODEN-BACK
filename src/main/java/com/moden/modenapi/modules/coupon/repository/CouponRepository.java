package com.moden.modenapi.modules.coupon.repository;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.coupon.model.Coupon;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends BaseRepository<Coupon, UUID> {

    // 이미 있는 거 (assignToCustomer에서 사용)
    Optional<Coupon> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * Berilgan studioda, bugungi kunda ishlatish mumkin bo'lgan
     * BIRTHDAY coupon (template) ni topish.
     */
    @Query("""
        select c
        from Coupon c
        where c.studioId = :studioId
          and c.deletedAt is null
          and c.birthdayCoupon = true
          and c.status = com.moden.modenapi.common.enums.CouponStatus.AVAILABLE
          and (c.startDate is null or c.startDate <= :today)
          and (c.expiryDate is null or c.expiryDate >= :today)
    """)
    Optional<Coupon> findActiveBirthdayCouponForStudio(
            @Param("studioId") UUID studioId,
            @Param("today") LocalDate today
    );

    // (agar customerga bog‘langan eski kodlar hali ishlatilayotgan bo‘lsa)
    List<Coupon> findAllByUserIdAndDeletedAtIsNull(UUID userId);
    List<Coupon> findAllByUserIdAndStatusAndDeletedAtIsNull(UUID userId, CouponStatus status);

    // 🔹 Studio bo‘yicha kupon policy ro‘yxati
    List<Coupon> findAllByStudioIdAndDeletedAtIsNull(UUID studioId);

    // 🔹 Studio + status bo‘yicha kupon policy ro‘yxati
    List<Coupon> findAllByStudioIdAndStatusAndDeletedAtIsNull(UUID studioId, CouponStatus status);
}
