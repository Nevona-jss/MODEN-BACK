package com.moden.modenapi.common.model;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
@MappedSuperclass
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate @Column(nullable = false, updatable = false)
    protected Instant createdAt;

    @LastModifiedDate @Column(nullable = false)
    protected Instant updatedAt;
}
