package com.moden.modenapi.modules.notify.model;

import com.moden.modenapi.common.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="notification")
public class NotificationTmpl {
    @Id @GeneratedValue @UuidGenerator @Column(columnDefinition="uniqueidentifier")
    private UUID id;

    @Column(nullable=false) private String title;
    @Lob @Column(nullable=false) private String content;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private NotificationType type;

    @Builder.Default @Column(nullable=false) private Instant createdAt = Instant.now();
}
