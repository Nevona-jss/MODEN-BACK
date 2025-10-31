package com.moden.modenapi.modules.point.repository;

import com.moden.modenapi.common.enums.PointType;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.point.model.Point;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PointRepository extends BaseRepository<Point, UUID> {


    List<Point> findAllByTypeAndDeletedAtIsNull(PointType type);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Point p WHERE p.type = 'EARN' AND p.deletedAt IS NULL")
    Double getTotalEarned();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Point p WHERE p.type = 'USE' AND p.deletedAt IS NULL")
    Double getTotalUsed();

    // 🔹 Find all points for a specific user
    @Query("SELECT p FROM Point p WHERE p.userId = :userId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<Point> findAllByUserIdAndDeletedAtIsNull(UUID userId);

    // 🔹 Find all points for a user filtered by type (적립/사용)
    @Query("SELECT p FROM Point p WHERE p.userId = :userId AND p.type = :type AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<Point> findAllByUserIdAndTypeAndDeletedAtIsNull(UUID userId, PointType type);

//    // 🔹 Find all by userId (for linking to specific payment)
//    @Query("SELECT p FROM Point p WHERE p.paymentId = :paymentId AND p.deletedAt IS NULL")
//    List<Point> findAllByPaymentIdAndDeletedAtIsNull(@Param("paymentId") UUID paymentId);

    List<Point> findAllByPaymentIdAndDeletedAtIsNull(UUID paymentId);

    // 🔹 Find by ID (soft delete aware)
    Optional<Point> findByIdAndDeletedAtIsNull(UUID id);
}
