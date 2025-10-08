package com.moden.modenapi.modules.studio.repository;

import java.util.UUID;

import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HairStudioDetailRepository extends JpaRepository<HairStudioDetail, UUID> {}
