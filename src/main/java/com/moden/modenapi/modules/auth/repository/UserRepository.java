package com.moden.modenapi.modules.auth.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.auth.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends BaseRepository<User, UUID> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByFullNameAndPhone(String fullname, String phone);

}
