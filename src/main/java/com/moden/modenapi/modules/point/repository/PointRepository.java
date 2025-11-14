package com.moden.modenapi.modules.point.repository;

import com.moden.modenapi.common.enums.PointType;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.point.model.Point;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PointRepository extends BaseRepository<Point, UUID> {

    // 🔹 전체 type별 (관리용)
    List<Point> findAllByTypeAndDeletedAtIsNull(PointType type);

    // 🔹 전체 시스템 기준 total (필요하면 사용)
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Point p
        WHERE p.type = com.moden.modenapi.common.enums.PointType.EARNED
          AND p.deletedAt IS NULL
    """)
    BigDecimal getTotalEarned();

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Point p
        WHERE p.type = com.moden.modenapi.common.enums.PointType.USED
          AND p.deletedAt IS NULL
    """)
    BigDecimal getTotalUsed();

    // 🔹 특정 user 전체 히스토리
    @Query("""
        SELECT p
        FROM Point p
        WHERE p.userId = :userId
          AND p.deletedAt IS NULL
        ORDER BY p.createdAt DESC
    """)
    List<Point> findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            @Param("userId") UUID userId
    );

    // 🔹 특정 user + type (EARN / USE)
    @Query("""
        SELECT p
        FROM Point p
        WHERE p.userId = :userId
          AND p.type   = :type
          AND p.deletedAt IS NULL
        ORDER BY p.createdAt DESC
    """)
    List<Point> findAllByUserIdAndTypeAndDeletedAtIsNull(
            @Param("userId") UUID userId,
            @Param("type") PointType type
    );

    // 🔹 특정 payment 기준
    List<Point> findAllByPaymentIdAndDeletedAtIsNull(UUID paymentId);

    // 🔹 soft delete aware 단건 조회
    Optional<Point> findByIdAndDeletedAtIsNull(UUID id);

    // 🔹 특정 user 기준 earned / used 합계
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Point p
        WHERE p.userId = :userId
          AND p.type   = com.moden.modenapi.common.enums.PointType.EARNED
          AND p.deletedAt IS NULL
    """)
    BigDecimal sumEarnedByUser(@Param("userId") UUID userId);

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Point p
        WHERE p.userId = :userId
          AND p.type   = com.moden.modenapi.common.enums.PointType.USED
          AND p.deletedAt IS NULL
    """)
    BigDecimal sumUsedByUser(@Param("userId") UUID userId);
}
