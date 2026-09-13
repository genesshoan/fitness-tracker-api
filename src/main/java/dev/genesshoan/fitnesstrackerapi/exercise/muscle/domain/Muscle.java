package dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import dev.genesshoan.fitnesstrackerapi.common.domain.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Represents a target muscle group.
 *
 * <p>Muscles are organized by {@link BodyRegion} and are used to
 * filter exercises and track target/stabilizer relationships
 * in training sessions.
 */
@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Table(name = "muscles")
public class Muscle extends BaseEntity {

    /**
     * Unique muscle name.
     */
    @Column(unique = true, nullable = false)
    private String name;

    /**
     * URL-friendly identifier used for lookup operations.
     */
    @Column(unique = true, nullable = false)
    private String slug;

    /**
     * The body region where this muscle is located.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BodyRegion bodyRegion;
}
