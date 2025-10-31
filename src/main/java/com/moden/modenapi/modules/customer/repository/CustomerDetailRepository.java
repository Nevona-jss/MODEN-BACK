package com.moden.modenapi.modules.customer.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerDetailRepository extends BaseRepository<CustomerDetail, UUID> {

    @Query("SELECT d FROM CustomerDetail d WHERE d.userId = :userId AND d.deletedAt IS NULL")
    Optional<CustomerDetail> findByUserId(UUID userId);

    @Query("SELECT d FROM CustomerDetail d WHERE MONTH(d.birthdate) = :month AND DAY(d.birthdate) = :day AND d.deletedAt IS NULL")
    List<CustomerDetail> findAllByBirthDateMonthAndDay(int month, int day);

}

