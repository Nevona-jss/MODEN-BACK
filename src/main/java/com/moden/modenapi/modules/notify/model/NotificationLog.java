package com.moden.modenapi.modules.notify.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.modules.auth.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name="notification_log", indexes=@Index(name="ix_nlog_user", columnList="customer_id"))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NotificationLog {
    @Id @GeneratedValue @UuidGenerator
    @Column(columnDefinition="uniqueidentifier")
    private UUID id;

    @ManyToOne(optional=false)
    @JoinColumn(name="notification_id")
    private NotificationTmpl notification;

    @ManyToOne(optional=false)
    @JoinColumn(name="customer_id")
    @JsonIgnoreProperties({"reservationsAsCustomer", "designerDetail"})
    private User customer;

    @Builder.Default @Column(nullable=false)
    private Instant sentAt = Instant.now();

    @Builder.Default @Column(nullable=false, length=20)
    private String status = "SENT";
}
