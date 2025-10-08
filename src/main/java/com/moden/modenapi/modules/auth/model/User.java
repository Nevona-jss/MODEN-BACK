package com.moden.modenapi.modules.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.enums.Gender;
import com.moden.modenapi.common.enums.UserType;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="users", uniqueConstraints=@UniqueConstraint(columnNames={"phone"}))
@JsonIgnoreProperties({
        "hibernateLazyInitializer",
        "handler",
        "reservations",
        "designerDetail",
        "passwordHash"
})
public class User extends BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition="uniqueidentifier")
    private UUID id;

    @Column(nullable=false, length=100)
    private String name;

    @Column(nullable=false, length=20)
    private String phone;

    @Column(length=255)
    private String email;

    private LocalDate birthdate;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private UserType userType = UserType.CUSTOMER;

    @Column(nullable=false)
    private boolean consentMarketing = false;

    private String naverId;
}
