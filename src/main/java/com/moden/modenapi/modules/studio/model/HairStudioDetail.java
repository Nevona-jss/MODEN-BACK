package com.moden.modenapi.modules.studio.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="hair_studio_detail")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HairStudioDetail {

    @Id
    @Column(name="hair_studio_id", columnDefinition="uniqueidentifier")
    private java.util.UUID id;

    @OneToOne(fetch=FetchType.LAZY)
    @MapsId
    @JoinColumn(name="hair_studio_id")
    @JsonIgnoreProperties({"detail", "reservations", "designers"}) // ⚙️ recursion to‘xtatadi
    private HairStudio studio;

    @Lob
    private String description;
    @Column(length=200)
    private String parkingInfo;
    private String naverUrl;
    private String blogUrl;
    private String instagramUrl;

    @Column(columnDefinition="nvarchar(max)")
    private String openHoursJson;
}
