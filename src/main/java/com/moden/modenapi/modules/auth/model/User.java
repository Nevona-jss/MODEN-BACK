package com.moden.modenapi.modules.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = {"phone"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User  extends BaseEntity {

    @Column(length = 150)
    protected String fullName;   // ✅ shared across entities (user, studio, etc.)

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

}
