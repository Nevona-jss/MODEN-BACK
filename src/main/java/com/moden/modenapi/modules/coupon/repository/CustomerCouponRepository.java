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

    List<CustomerCoupon> findAllByCustomerIdAndStatusAndDeletedAtIsNull(UUID customerId, CouponStatus status);

    boolean existsByCouponIdAndCustomerIdAndStatusIn(UUID couponId,
                                                     UUID customerId,
                                                     List<CouponStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CustomerCoupon c where c.id = :id and c.deletedAt is null")
    Optional<CustomerCoupon> lockByIdForUpdate(@Param("id") UUID id);
}
