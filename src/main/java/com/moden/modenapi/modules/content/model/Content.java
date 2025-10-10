package com.moden.modenapi.modules.content.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.enums.NotificationType;
import com.moden.modenapi.modules.serviceitem.model.StudioServiceItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name="content")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Content {
    @Id @GeneratedValue @UuidGenerator
    @Column(columnDefinition="uniqueidentifier")
    private UUID id;

    @Column(nullable=false) private String title;
    @Lob private String content;
    private String media;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @ManyToOne
    @JoinColumn(name="service_id")
    @JsonIgnoreProperties({"studio"})
    private StudioServiceItem service;

    @Builder.Default
    @Column(nullable=false)
    private Instant createdAt = Instant.now();
}
