package com.moden.modenapi.modules.qa.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.qa.model.QA;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QARepository extends BaseRepository<QA, UUID> {

    // 고객: 내 문의 목록
    List<QA> findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

    // 단건 (soft delete aware)
    Optional<QA> findByIdAndDeletedAtIsNull(UUID id);

    // 스튜디오/디자이너: 내 스튜디오 고객들의 문의 전체
    @Query("""
        SELECT q
        FROM QA q
        WHERE q.deletedAt IS NULL
          AND q.userId IN (
              SELECT c.userId
              FROM CustomerDetail c
              WHERE c.studioId = :studioId
                AND c.deletedAt IS NULL
          )
        ORDER BY COALESCE(q.answeredAt, q.createdAt) DESC
    """)
    List<QA> findAllForStudio(@Param("studioId") UUID studioId);



}
