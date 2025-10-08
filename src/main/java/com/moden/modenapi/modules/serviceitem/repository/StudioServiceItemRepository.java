package com.moden.modenapi.modules.serviceitem.repository;

import com.moden.modenapi.modules.serviceitem.model.StudioServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface StudioServiceItemRepository extends JpaRepository<StudioServiceItem, UUID> {}
