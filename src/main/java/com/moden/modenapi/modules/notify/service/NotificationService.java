package com.moden.modenapi.modules.notify.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.notify.dto.NotificationRes;
import com.moden.modenapi.modules.notify.model.Notification;
import com.moden.modenapi.modules.notify.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/**
 * Service handling notifications for users.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService extends BaseService<Notification, UUID> {

    private final NotificationRepository repo;

    @Override
    protected JpaRepository<Notification, UUID> getRepository() {
        return repo;
    }

    /**
     * Returns all notifications for a specific user.
     */

    @Transactional(readOnly = true)
    public List<NotificationRes> listByUser(UUID userId) {
        return repo.findAllByUser_Id(userId).stream()
                .map(NotificationRes::from)
                .toList();
    }

}
