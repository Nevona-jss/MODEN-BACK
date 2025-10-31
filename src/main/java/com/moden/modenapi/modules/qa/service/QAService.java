package com.moden.modenapi.modules.qa.service;

import com.moden.modenapi.common.enums.QAStatus;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.qa.dto.QAResponse;
import com.moden.modenapi.modules.qa.dto.QACreateRequest;
import com.moden.modenapi.modules.qa.model.QA;
import com.moden.modenapi.modules.qa.repository.QARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class QAService extends BaseService<QA> {

    private final QARepository qaRepository;

    @Override
    protected QARepository getRepository() {
        return qaRepository;
    }

    // 고객 문의 등록
    public QAResponse createQuestion(UUID userId, QACreateRequest req) {
        QA qa = QA.builder()
                .userId(userId)
                .title(req.title())
                .content(req.content())
                .status(QAStatus.PENDING)
                .build();

        qaRepository.save(qa);
        return mapToRes(qa);
    }

    // 스튜디오 답변 등록
    public QAResponse answerQuestion(UUID studioUserId, UUID qaId, String answer) {
        QA qa = qaRepository.findById(qaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문의 내역이 없습니다."));

        if (qa.getStatus() == QAStatus.CLOSED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 종료된 문의입니다.");

        qa.setAnswer(answer);
        qa.setAnsweredBy(studioUserId);
        qa.setAnsweredAt(Instant.now());
        qa.setStatus(QAStatus.ANSWERED);
        qaRepository.save(qa);

        return mapToRes(qa);
    }

    // 고객 문의 리스트
    @Transactional(readOnly = true)
    public List<QAResponse> getUserQuestions(UUID userId) {
        return qaRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToRes)
                .collect(Collectors.toList());
    }

    private QAResponse mapToRes(QA qa) {
        return new QAResponse(
                qa.getId(),
                qa.getUserId(),
                qa.getTitle(),
                qa.getContent(),
                qa.getStatus(),
                qa.getAnswer(),
                qa.getAnsweredAt(),
                qa.getCreatedAt()
        );
    }
}
