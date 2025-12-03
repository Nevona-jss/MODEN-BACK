package com.moden.modenapi.modules.coupon.repository;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.coupon.model.CustomerCoupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface CustomerCouponRepository extends BaseRepository<CustomerCoupon, UUID> {
    Optional<CustomerCoupon> findByIdAndDeletedAtIsNull(UUID id);

    List<CustomerCoupon> findAllByCustomerIdAndDeletedAtIsNull(UUID customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CustomerCoupon c where c.id = :id and c.deletedAt is null")
    Optional<CustomerCoupon> lockByIdForUpdate(@Param("id") UUID id);

    // ✅ customerId 기준 전체 + createdAt 내림차순
    List<CustomerCoupon> findAllByCustomerIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID customerId);

    /**
     * 같은 couponId + customerId 조합의 살아있는 row 가 이미 있는지 검사
     *  → 생일쿠폰, 첫방문쿠폰 중복 발급 방지 용도
     */
    boolean existsByCouponIdAndCustomerIdAndDeletedAtIsNull(UUID couponId, UUID customerId);

    /**
     * soft delete 고려한 단건 조회
     */


}
