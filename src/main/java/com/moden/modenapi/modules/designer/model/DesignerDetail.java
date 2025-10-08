package com.moden.modenapi.modules.designer.model;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.modules.studio.model.HairStudio;
import jakarta.persistence.*;
import lombok.*;
import com.moden.modenapi.modules.auth.model.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "userId")
@Entity
@Table(name="designer_detail")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DesignerDetail {

    @Id
    @Column(name="user_id", columnDefinition="uniqueidentifier")
    private UUID userId;

    @OneToOne(fetch=FetchType.LAZY)
    @MapsId
    @JoinColumn(name="user_id")
    @JsonIgnoreProperties({"designerDetail"})  // 👈 loopdan saqlaydi
    private User user; // must be UserType.DESIGNER

    @ManyToOne(optional=false)
    @JoinColumn(name="hair_studio_id")
    @JsonIgnoreProperties({"designers", "reservations"})  // 👈 loopdan saqlaydi
    private HairStudio hairStudio;

    private String portfolioUrl;
    private String phonePublic;

    @Lob
    private String bio;

    @Builder.Default
    private Instant createdAt = Instant.now();
    private Instant updatedAt;
}
