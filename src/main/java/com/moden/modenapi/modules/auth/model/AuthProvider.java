package com.moden.modenapi.modules.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name="auth_provider",
        uniqueConstraints=@UniqueConstraint(columnNames={"provider","provider_uid"})
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuthProvider {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition="uniqueidentifier")
    private UUID id;

    @ManyToOne(optional=false)
    @JoinColumn(name="user_id")
    @JsonIgnoreProperties({"authProviders", "designerDetail"})
    private User user;

    @Column(nullable=false, length=30)
    private String provider;

    @Column(name="provider_uid", nullable=false, length=200)
    private String providerUid;

    @Builder.Default
    @Column(nullable=false)
    private Instant linkedAt = Instant.now();
}
