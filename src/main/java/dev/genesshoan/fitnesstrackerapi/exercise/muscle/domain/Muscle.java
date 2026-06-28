package dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain;

import dev.genesshoan.fitnesstrackerapi.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Table(name = "muscles")
public class Muscle extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BodyRegion bodyRegion;
}
