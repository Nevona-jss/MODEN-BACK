package com.moden.modenapi.modules.designer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.enums.DesignerStatus;
import com.moden.modenapi.common.enums.Position;
import com.moden.modenapi.common.enums.Weekday;
import com.moden.modenapi.common.model.BaseEntity;
import com.moden.modenapi.common.utils.UuidListJsonConverter;
import com.moden.modenapi.common.utils.WeekdayToIntConverter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
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
    private String idForLogin;

    @Column(length = 1000)
    private String bio;

    @Enumerated(EnumType.STRING)
    private Position position = Position.DESIGNER;

    @Column(name = "portfolio_item_ids", columnDefinition = "nvarchar(max)")
    @Convert(converter = UuidListJsonConverter.class)
    private List<UUID> portfolioItemIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private DesignerStatus status = DesignerStatus.WORKING;  // default: 근무중

    @ElementCollection
    @CollectionTable(name = "designer_days_off", joinColumns = @JoinColumn(name = "designer_id"))
    @Column(name = "day_code")
    @Convert(converter = WeekdayToIntConverter.class)  // store 0..6
    private List<Weekday> daysOff = new ArrayList<>();


}
