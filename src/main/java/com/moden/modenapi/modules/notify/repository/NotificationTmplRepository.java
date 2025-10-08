package com.moden.modenapi.modules.notify.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.moden.modenapi.modules.notify.model.NotificationTmpl;

public interface NotificationTmplRepository extends JpaRepository<NotificationTmpl, UUID> {}
