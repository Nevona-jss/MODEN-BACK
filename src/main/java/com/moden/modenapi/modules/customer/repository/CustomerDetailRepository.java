package com.moden.modenapi.modules.customer.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerDetailRepository extends BaseRepository<CustomerDetail, UUID> {

    @Query("select c from CustomerDetail c where c.userId = :userId and c.deletedAt is null")
    Optional<CustomerDetail> findActiveByUserId(@Param("userId") UUID userId);

    @Query("""
        select c from CustomerDetail c
        where c.studioId = :studioId and c.deletedAt is null
        order by coalesce(c.updatedAt, c.createdAt) desc
    """)
    List<CustomerDetail> findAllActiveByStudio(@Param("studioId") UUID studioId);

    @Query("""
        select c from CustomerDetail c
        where c.deletedAt is null
          and (
               c.designerId = :designerDetailId
            or c.designerId = :designerUserId
          )
        order by coalesce(c.updatedAt, c.createdAt) desc
    """)
    List<CustomerDetail> findAllActiveForDesigner(
            @Param("designerDetailId") UUID designerDetailId,
            @Param("designerUserId")   UUID designerUserId
    );

    @Query("""
        select c from CustomerDetail c
        where c.userId = :customerUserId
          and c.studioId = :studioId
          and c.deletedAt is null
    """)
    Optional<CustomerDetail> findOneActiveInStudio(
            @Param("customerUserId") UUID customerUserId,
            @Param("studioId")       UUID studioId
    );

    @Query("""
        select c from CustomerDetail c
        where c.userId = :customerUserId
          and (c.designerId = :designerDetailId or c.designerId = :designerUserId)
          and c.deletedAt is null
    """)
    Optional<CustomerDetail> findOneActiveForDesigner(
            @Param("customerUserId")   UUID customerUserId,
            @Param("designerDetailId") UUID designerDetailId,
            @Param("designerUserId")   UUID designerUserId
    );

    @Query("""
        select c
        from CustomerDetail c
        where c.userId = :userId and c.deletedAt is null
        order by coalesce(c.updatedAt, c.createdAt) desc
    """)
    java.util.List<CustomerDetail> findActiveByUserIdOrderByUpdatedDesc(
            @Param("userId") UUID userId,
            Pageable pageable
    );




    @Query("""
    select c.id, c.userId, u.fullName, u.phone, c.profileImageUrl, c.createdAt, c.updatedAt
    from CustomerDetail c
    left join User u on u.id = c.userId
    left join DesignerDetail d on d.id = c.designerId and d.deletedAt is null
    where c.deletedAt is null
      and (
            c.studioId = :studioId               
         or (d is not null and d.hairStudioId = :studioId)  
      )
    order by coalesce(c.updatedAt, c.createdAt) desc
""")
    List<Object[]> findCustomerRowsForStudio(@Param("studioId") UUID studioId);


    @Query("SELECT d FROM CustomerDetail d WHERE d.userId = :userId AND d.deletedAt IS NULL")
    Optional<CustomerDetail> findByUserId(UUID userId);

    @Query("SELECT d FROM CustomerDetail d WHERE MONTH(d.birthdate) = :month AND DAY(d.birthdate) = :day AND d.deletedAt IS NULL")
    List<CustomerDetail> findAllByBirthDateMonthAndDay(int month, int day);

}
