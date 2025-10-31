package com.moden.modenapi.modules.designer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.enums.Position;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "designer_detail")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DesignerDetail extends BaseEntity {

    @Column(name = "user_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID userId;

    @Column(name = "hair_studio_id", columnDefinition = "uniqueidentifier", nullable = false)
    private UUID hairStudioId;

    @Column(length = 50, unique = true)
    private String idForLogin; // Designer login code (e.g., DS-XXX-12345)

    @Column(length = 1000)
    private String bio;

    @Column(length = 500)
    private String portfolioUrl; //ko'p rasm yuklanishi mumkin

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.DESIGNER; // ✅ FIXED — now builder & getter exist

    @Enumerated(EnumType.STRING)
    private Position position = Position.DESIGNER;


}
