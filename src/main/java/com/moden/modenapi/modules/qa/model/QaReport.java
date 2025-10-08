package com.moden.modenapi.modules.qa.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="qa_report")
public class QaReport {
    @Id @GeneratedValue @UuidGenerator @Column(columnDefinition="uniqueidentifier")
    private UUID id;

    @Column(nullable=false) private String title;
    private String testerName;
    private Instant testDate;
    private String status;     // PASSED / FAILED / NEED_FIX

    @Column(columnDefinition="TEXT")
    private String summary;
}
