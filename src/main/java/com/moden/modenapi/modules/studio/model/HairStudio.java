package com.moden.modenapi.modules.studio.model;

import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="hair_studio")
public class HairStudio extends BaseEntity {
    @Id @GeneratedValue @UuidGenerator @Column(columnDefinition="uniqueidentifier")
    private UUID id;

    @Column(nullable=false, length=150) private String name;
    private String qrCodeUrl;
    private String businessNo;
    private String address;
    private String phone;
}
