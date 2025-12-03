package com.moden.modenapi.modules.coupon.repository;

import com.moden.modenapi.common.enums.CouponStatus;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.coupon.model.Coupon;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends BaseRepository<Coupon, UUID> {



    // 이미 쓰고 있던 메서드
    Optional<Coupon> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * 스튜디오에서 사용 중인 생일 쿠폰 템플릿 찾기
     */
    @Query("""
        select c
        from Coupon c
        where c.studioId = :studioId
          and c.deletedAt is null
          and c.status = com.moden.modenapi.common.enums.CouponStatus.AVAILABLE
          and (c.startDate is null or c.startDate <= :today)
          and (c.expiryDate is null or c.expiryDate >= :today)
    """)
    Optional<Coupon> findActiveBirthdayCouponForStudio(
            @Param("studioId") UUID studioId,
            @Param("today") LocalDate today
    );

    // 🔹 Studio 기준 정책 쿠폰들
    List<Coupon> findAllByStudioIdAndDeletedAtIsNull(UUID studioId);

    List<Coupon> findAllByStudioIdAndStatusAndDeletedAtIsNull(UUID studioId, CouponStatus status);

    // ===========================================================
    // 🔥 여기부터가 네가 원한 메서드 2개 (userId 기준 조회)
    //  - Coupon 엔티티에 userId 필드 없어도, JPQL join 으로 해결
    //  - userId = CustomerDetail.userId 기준으로 고객이 가진 쿠폰 추출
    // ===========================================================

    /**
     * 특정 userId(고객 유저) 가 가진 모든 쿠폰 (상태 상관 없음)
     *  - CustomerDetail.userId -> CustomerCoupon.customerId -> Coupon
     */
    @Query("""
        select c
        from Coupon c
          join CustomerCoupon cc on cc.couponId = c.id
          join CustomerDetail cd on cd.id = cc.customerId
        where cd.userId = :userId
          and c.deletedAt is null
    """)
    List<Coupon> findAllByUserIdAndDeletedAtIsNull(@Param("userId") UUID userId);

    /**
     * 특정 userId(고객 유저) 가 가진 쿠폰 중, 특정 상태만
     */
    @Query("""
        select c
        from Coupon c
          join CustomerCoupon cc on cc.couponId = c.id
          join CustomerDetail cd on cd.id = cc.customerId
        where cd.userId = :userId
          and c.status = :status
          and c.deletedAt is null
    """)
    List<Coupon> findAllByUserIdAndStatusAndDeletedAtIsNull(
            @Param("userId") UUID userId,
            @Param("status") CouponStatus status
    );
}
