package com.moden.modenapi.modules.qa.service;

import com.moden.modenapi.common.enums.QAStatus;
import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import com.moden.modenapi.modules.designer.model.DesignerDetail;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.qa.dto.QAAnswerRequest;
import com.moden.modenapi.modules.qa.dto.QACreateRequest;
import com.moden.modenapi.modules.qa.dto.QAResponse;
import com.moden.modenapi.modules.qa.model.QA;
import com.moden.modenapi.modules.qa.repository.QARepository;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class QAService extends BaseService<QA> {

    private final QARepository qaRepository;
    private final UserRepository userRepository;
    private final CustomerDetailRepository customerDetailRepository;
    private final HairStudioDetailRepository hairStudioDetailRepository;
    private final DesignerDetailRepository designerDetailRepository;

    @Override
    protected QARepository getRepository() {
        return qaRepository;
    }

    /* ============= Mapper ============= */

    private QAResponse toResponse(QA qa) {
        User customerUser = userRepository.findById(qa.getUserId()).orElse(null);
        String customerName  = customerUser != null ? customerUser.getFullName() : null;
        String customerPhone = customerUser != null ? customerUser.getPhone()     : null;

        String answeredByName = null;
        if (qa.getAnsweredBy() != null) {
            answeredByName = userRepository.findById(qa.getAnsweredBy())
                    .map(User::getFullName)
                    .orElse(null);
        }

        return new QAResponse(
                qa.getId(),
                qa.getUserId(),
                customerName,
                customerPhone,
                qa.getTitle(),
                qa.getContent(),
                qa.getStatus(),
                qa.getAnswer(),
                answeredByName,
                qa.getCreatedAt(),
                qa.getAnsweredAt()
        );
    }

    /* ============= CUSTOMER SIDE ============= */

    // 고객: 새 문의 작성
    public QAResponse createByCustomer(UUID userId, QACreateRequest req) {
        QA qa = QA.builder()
                .userId(userId)
                .title(req.title())
                .content(req.content())
                .status(QAStatus.PENDING)
                .build();

        qa = create(qa);
        return toResponse(qa);
    }

    // 고객: 내 문의 목록
    @Transactional(readOnly = true)
    public List<QAResponse> listForCustomer(UUID userId) {
        return qaRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 고객: 내 문의 상세
    @Transactional(readOnly = true)
    public QAResponse getForCustomer(UUID userId, UUID qaId) {
        QA qa = qaRepository.findByIdAndDeletedAtIsNull(qaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inquiry not found"));

        if (!qa.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This inquiry does not belong to current user");
        }
        return toResponse(qa);
    }

    /* ============= STUDIO/DESIGNER SIDE ============= */

    // 현재 유저가 속한 스튜디오 ID (HAIR_STUDIO or DESIGNER)
    private UUID resolveStudioIdForUser(UUID currentUserId) {
        // 1) 디자이너인지 먼저 확인
        DesignerDetail designer = designerDetailRepository.findByUserIdAndDeletedAtIsNull(currentUserId)
                .orElse(null);
        if (designer != null) {
            return designer.getHairStudioId();
        }

        // 2) 스튜디오 오너
        var studios = hairStudioDetailRepository
                .findActiveByUserIdOrderByUpdatedDesc(currentUserId, PageRequest.of(0, 1));
        if (studios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Studio not found for current user");
        }
        return studios.get(0).getId();
    }

    // 스튜디오/디자이너: 내 스튜디오 고객들의 문의 리스트
    @Transactional(readOnly = true)
    public List<QAResponse> listForStudio(UUID currentUserId) {
        UUID studioId = resolveStudioIdForUser(currentUserId);
        return qaRepository.findAllForStudio(studioId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 스튜디오/디자이너: 문의 상세 (내 스튜디오 고객 것만)
    @Transactional(readOnly = true)
    public QAResponse getForStudio(UUID currentUserId, UUID qaId) {
        UUID studioId = resolveStudioIdForUser(currentUserId);

        QA qa = qaRepository.findByIdAndDeletedAtIsNull(qaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inquiry not found"));

        // 이 문의가 내 스튜디오 고객의 것인지 확인
        var customerOpt = customerDetailRepository
                .findActiveByUserIdOrderByUpdatedDesc(qa.getUserId(), PageRequest.of(0, 1))
                .stream()
                .findFirst();

        if (customerOpt.isEmpty() || !studioId.equals(customerOpt.get().getStudioId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inquiry does not belong to your studio");
        }



        return toResponse(qa);
    }

    // 스튜디오/디자이너: 답변 작성 (1번만, 채팅 아님)
    public QAResponse answerByStudio(UUID currentUserId, UUID qaId, QAAnswerRequest req) {
        UUID studioId = resolveStudioIdForUser(currentUserId);

        QA qa = qaRepository.findByIdAndDeletedAtIsNull(qaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inquiry not found"));

        // 스튜디오 소속 고객 문의인지 확인
        var customerOpt = customerDetailRepository
                .findActiveByUserIdOrderByUpdatedDesc(qa.getUserId(), PageRequest.of(0, 1))
                .stream()
                .findFirst();
        if (customerOpt.isEmpty() || !studioId.equals(customerOpt.get().getStudioId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inquiry does not belong to your studio");
        }

        // 이미 ANSWERED 인데 다시 쓰고 싶으면 여기서 막을 수도 있음
        // if (qa.getStatus() == QAStatus.ANSWERED) { ... }

        qa.setAnswer(req.answer());
        qa.setAnsweredAt(Instant.now());
        qa.setAnsweredBy(currentUserId);
        qa.setStatus(QAStatus.ANSWERED);

        qa = update(qa);
        return toResponse(qa);
    }
}
