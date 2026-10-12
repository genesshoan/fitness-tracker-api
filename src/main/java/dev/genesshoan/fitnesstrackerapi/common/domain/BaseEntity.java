package dev.genesshoan.fitnesstrackerapi.common.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import org.springframework.data.domain.Persistable;

import com.github.f4b6a3.uuid.UuidCreator;
import lombok.AccessLevel;
import lombok.Builder;
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
public abstract class BaseEntity implements Persistable<UUID> {

    @Id
    @Column(updatable = false, nullable = false, unique = true)
    @Builder.Default
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Transient
    @Builder.Default
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseEntity that = (BaseEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }
}
