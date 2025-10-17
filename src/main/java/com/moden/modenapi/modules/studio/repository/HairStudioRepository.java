package com.moden.modenapi.modules.studio.repository;

import java.util.Optional;
import java.util.UUID;

import com.moden.modenapi.modules.studio.model.HairStudio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HairStudioRepository extends JpaRepository<HairStudio, UUID> {
    Optional<HairStudio> findByIdForLogin(String idForLogin);
}
