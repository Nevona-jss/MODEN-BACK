package com.moden.modenapi.modules.booking.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.studio.model.HairStudio;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "schedule", uniqueConstraints = @UniqueConstraint(columnNames = {"designer_id", "day"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Schedule {
    @Id @GeneratedValue @UuidGenerator
    @Column(columnDefinition = "uniqueidentifier")
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "designer_id")
    @JsonIgnoreProperties({"reservationsAsDesigner", "designerDetail"})
    private User designer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hair_studio_id")
    @JsonIgnoreProperties({"services", "reservations", "designers"})
    private HairStudio studio;

    @Column(nullable = false)
    private LocalDate day;

    @Column(name = "available_time", columnDefinition = "nvarchar(max)", nullable = false)
    private String availableTimeJson;
}
