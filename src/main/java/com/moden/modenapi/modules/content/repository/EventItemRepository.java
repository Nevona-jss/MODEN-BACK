package com.moden.modenapi.modules.content.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.moden.modenapi.modules.content.model.EventItem;

public interface EventItemRepository extends JpaRepository<EventItem, UUID> {}
