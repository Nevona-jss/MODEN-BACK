package com.moden.modenapi.modules.qa.repository;

import com.moden.modenapi.modules.qa.model.QaReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface QaReportRepository extends JpaRepository<QaReport, UUID> {}
