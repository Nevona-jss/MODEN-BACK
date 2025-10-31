package com.moden.modenapi.modules.event.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.event.model.Event;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends BaseRepository<Event, UUID> {

    @Query("SELECT e FROM Event e WHERE e.studioId = :studioId AND e.deletedAt IS NULL ORDER BY e.startDate DESC")
    List<Event> findAllActiveByStudioId(UUID studioId);

    @Query("SELECT e FROM Event e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<Event> findActiveById(UUID id);
}
