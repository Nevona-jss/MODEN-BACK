package com.moden.modenapi.modules.auth.repository;

import com.moden.modenapi.modules.auth.model.AuthLocal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthLocalRepository extends JpaRepository<AuthLocal, UUID> {

    Optional<AuthLocal> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
