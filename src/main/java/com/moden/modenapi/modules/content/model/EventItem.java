package com.moden.modenapi.modules.content.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="event")
public class EventItem {
    @Id @GeneratedValue @UuidGenerator @Column(columnDefinition="uniqueidentifier")
    private UUID id;

    @Column(nullable=false) private String title;
    @Lob private String description;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;

    @Builder.Default @Column(nullable=false) private Instant createdAt = Instant.now();
}
