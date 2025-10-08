package com.moden.modenapi.modules.qa.service;

import com.moden.modenapi.modules.qa.dto.QaReportDto;
import com.moden.modenapi.modules.qa.model.QaReport;
import com.moden.modenapi.modules.qa.repository.QaReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class QaService {

    private final QaReportRepository repo;

    public QaReport create(QaReportDto dto) {
        QaReport report = QaReport.builder()
                .title(dto.title())
                .testerName(dto.testerName())
                .status(dto.status())
                .summary(dto.summary())
                .build();

        return repo.save(report);
    }

    @Transactional(readOnly = true)
    public List<QaReport> list() {
        return repo.findAll();
    }
}
