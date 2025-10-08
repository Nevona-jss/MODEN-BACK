package com.moden.modenapi.modules.auth.repository;

import com.moden.modenapi.modules.auth.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {}
