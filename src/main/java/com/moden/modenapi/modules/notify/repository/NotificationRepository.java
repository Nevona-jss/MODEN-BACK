package com.moden.modenapi.modules.notify.repository;

import com.moden.modenapi.modules.notify.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    /**
     * Returns all notifications belonging to a specific user.
     */
    List<Notification> findAllByUser_Id(UUID userId);  // ✅ Query-level filtering
}