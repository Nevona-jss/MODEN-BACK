package com.moden.modenapi.modules.consult.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.moden.modenapi.modules.consult.model.Consultation;

public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {}
