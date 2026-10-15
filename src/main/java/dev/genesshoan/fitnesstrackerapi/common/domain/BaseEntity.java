package dev.genesshoan.fitnesstrackerapi.common.domain;

import java.time.Instant;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Base entity for all JPA entities in the system.
 * Provides common fields such as id, creation timestamp, and update timestamp.
 * These fields are managed automatically by Hibernate.
 */
@Getter
@Setter
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
public abstract class BaseEntity extends IdentifiableEntity {

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
