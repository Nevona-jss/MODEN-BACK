package com.moden.modenapi.modules.auth.repository;
import com.moden.modenapi.modules.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);
}
