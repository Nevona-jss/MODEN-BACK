package com.moden.modenapi.modules.qa.model;

import com.moden.modenapi.common.enums.QAStatus;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "qa_report")
public class QA extends BaseEntity {

    @Column(name = "user_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID userId;  // 문의 작성자 (고객 또는 스튜디오 사용자)

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private QAStatus status = QAStatus.PENDING; // WAITING, ANSWERED, CLOSED

    @Lob
    private String answer;

    private Instant answeredAt;

    @Column(columnDefinition = "uniqueidentifier")
    private UUID answeredBy; // optional: 답변 작성자 (헤어스튜디오 유저)
}
