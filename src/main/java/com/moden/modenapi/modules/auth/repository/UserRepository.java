package com.moden.modenapi.modules.auth.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.auth.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends BaseRepository<User, UUID> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByFullNameAndPhone(String fullname, String phone);

    @Query("""
    select u
    from User u
    where (u.fullName    like concat('%', :keyword, '%')
        or u.phone like concat('%', :keyword, '%'))
      and u.role = 'CUSTOMER'
""")
    List<User> searchCustomers(@Param("keyword") String keyword);


}
