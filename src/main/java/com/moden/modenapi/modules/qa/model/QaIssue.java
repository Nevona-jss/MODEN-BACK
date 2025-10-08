package com.moden.modenapi.modules.qa.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "qa_issue")
public class QaIssue {
    @Id @GeneratedValue @UuidGenerator
    @Column(columnDefinition = "uniqueidentifier")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private QaReport report;

    @Column(nullable = false)
    private String type; // UI, FUNCTIONAL, PERFORMANCE, SECURITY

    @Column(nullable = false)
    private String severity; // LOW / MEDIUM / HIGH / CRITICAL

    @Column(columnDefinition = "TEXT")
    private String description;

    private Instant createdAt = Instant.now();
}
