package com.moden.modenapi.modules.designer.repository;

import java.util.UUID;

import com.moden.modenapi.modules.designer.model.DesignerDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DesignerDetailRepository extends JpaRepository<DesignerDetail, UUID> {}
