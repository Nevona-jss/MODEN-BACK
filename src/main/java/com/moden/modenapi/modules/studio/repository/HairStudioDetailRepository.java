package com.moden.modenapi.modules.studio.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface HairStudioDetailRepository extends BaseRepository<HairStudioDetail, UUID> {

    Optional<HairStudioDetail> findByIdAndDeletedAtIsNull(UUID id);            // ✅ ishlatiladi
    Optional<HairStudioDetail> findByUserIdAndDeletedAtIsNull(UUID userId);    // ✅ ishlatiladi

    @Query("""
        select s from HairStudioDetail s
        where s.userId = :ownerUserId
          and s.deletedAt is null
    """)
    Optional<HairStudioDetail> findByOwnerUserId(@Param("ownerUserId") UUID ownerUserId); // ✅ resolveStudioIdForCurrentUser() da ishlatiladi

    @Query("""
        select s
        from HairStudioDetail s
        where s.userId = :userId and s.deletedAt is null
        order by coalesce(s.updatedAt, s.createdAt) desc
    """)
    List<HairStudioDetail> findActiveByUserIdOrderByUpdatedDesc(@Param("userId") UUID userId,
                                                                org.springframework.data.domain.Pageable pageable);

    // Quyidagilar faqat idForLogin maydoni bo'lsa kerak bo'ladi:
    boolean existsByIdForLogin(String idForLogin);
    boolean existsByIdForLoginIgnoreCase(String idForLogin);
    Optional<HairStudioDetail> findByIdForLogin(String idForLogin);
    Optional<HairStudioDetail> findByIdForLoginIgnoreCase(String idForLogin);
}
