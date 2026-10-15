package dev.genesshoan.fitnesstrackerapi.common.domain;

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
import org.hibernate.Hibernate;

@Getter
@Setter
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
public abstract class IdentifiableEntity implements Persistable<UUID> {

    @Id
    @Column(updatable = false, nullable = false, unique = true)
    @Builder.Default
    private UUID id = UuidCreator.getTimeOrderedEpoch();

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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        return getId().equals(((IdentifiableEntity) o).getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
