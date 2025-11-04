package com.moden.modenapi.modules.auth.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.common.repository.ProfileProjection;
import com.moden.modenapi.modules.auth.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends BaseRepository<User, UUID> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByFullNameAndPhone(String fullname, String phone);
    Optional<User> findByPhoneAndDeletedAtIsNull(String phone);
    boolean existsByPhone(String phone);


    // ✅ ADMIN (yoki role-agnostic) profilini to'g'ridan-to'g'ri users jadvalidan olish
    @Query("""
       select u.id as id,
              coalesce(u.fullName, u.phone, concat('', u.id)) as displayName
       from User u
       where u.id = :userId
    """)
    Optional<ProfileProjection> findProfileByUserId(@Param("userId") UUID userId);

    // (ixtiyoriy) phone bilan ham olish kerak bo'lsa:
    @Query("""
       select u.id as id,
              coalesce(u.fullName, u.phone, concat('', u.id)) as displayName
       from User u
       where u.phone = :phone and u.deletedAt is null
    """)
    Optional<ProfileProjection> findProfileByPhone(@Param("phone") String phone);
}
