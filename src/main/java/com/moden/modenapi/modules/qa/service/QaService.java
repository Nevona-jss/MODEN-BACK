package com.moden.modenapi.modules.qa.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.qa.dto.QaReportDto;
import com.moden.modenapi.modules.qa.model.QaReport;
import com.moden.modenapi.modules.qa.repository.QaReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing QA reports (test reports, verification logs, etc.)
 */
@Service
@Transactional
@RequiredArgsConstructor
public class QaService extends BaseService<QaReport, UUID> {

    private final QaReportRepository repo;

    @Override
    protected JpaRepository<QaReport, UUID> getRepository() {
        return repo;
    }

    /**
     * Create new QA report.
     */
    public QaReport create(QaReportDto dto) {
        var report = QaReport.builder()
                .title(dto.title())
                .testerName(dto.testerName())
                .status(dto.status())
                .summary(dto.summary())
                .build();
        return save(report);
    }

    /**
     * List all QA reports.
     */
    @Transactional(readOnly = true)
    public List<QaReport> list() {
        return getAll(); // ✅ Provided by BaseService
    }
}
