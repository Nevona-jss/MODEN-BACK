package com.moden.modenapi.modules.consult.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.model.BaseEntity;
import com.moden.modenapi.modules.booking.model.Reservation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name="consultation")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Consultation extends BaseEntity {
    @Id @GeneratedValue @UuidGenerator
    @Column(columnDefinition="uniqueidentifier")
    private UUID id;

    @ManyToOne(optional=true)
    @JoinColumn(name="reservation_id")
    @JsonIgnore // recursion to‘xtatadi
    private Reservation reservation;

    @Lob private String notes;
    private String beforeImage;
    private String afterImage;
}
