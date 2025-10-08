package com.moden.modenapi.modules.consult.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name="consultation_media")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ConsultationMedia {
    @Id @GeneratedValue @UuidGenerator
    @Column(columnDefinition="uniqueidentifier")
    private UUID id;

    @ManyToOne(optional=false)
    @JoinColumn(name="consultation_id")
    @JsonIgnoreProperties({"reservation"})
    private Consultation consultation;

    private String mediaUrl;
    @Enumerated(EnumType.STRING)
    private MediaType type;

    @Lob private byte[] media;
}
