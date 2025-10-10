package com.moden.modenapi.modules.notify.dto;

import com.moden.modenapi.common.enums.NotificationType;
import com.moden.modenapi.modules.notify.model.Notification;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for sending notifications to the client.
 * Prevents exposing full entity internals.
 */
public record NotificationRes(
        UUID id,
        String title,
        String content,
        NotificationType type,
        Instant createdAt
) {
    public static NotificationRes from(Notification n) {
        return new NotificationRes(
                n.getId(),
                n.getTitle(),
                n.getContent(),
                n.getType(),
                n.getCreatedAt()
        );
    }
}
