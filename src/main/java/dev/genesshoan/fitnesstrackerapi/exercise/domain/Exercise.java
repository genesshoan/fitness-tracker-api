package dev.genesshoan.fitnesstrackerapi.exercise.domain;

import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import dev.genesshoan.fitnesstrackerapi.common.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Represents an exercise in the catalog.
 *
 * <p>Each exercise is uniquely identified by its name and slug.
 * Exercises can be filtered by category, difficulty, and associated muscles.
 * The {@link #active} flag enables soft deletion: inactive exercises are
 * excluded from all read operations.
 *
 * <p>The {@link #exerciseMuscles} relationship stores the muscles involved
 * in the exercise along with their impact level (primary, secondary,
 * stabilizer).
 */
@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "exercises")
public class Exercise extends BaseEntity {

    /**
     * Unique exercise name.
     */
    @Column(unique = true, nullable = false)
    private String name;

    /**
     * URL-friendly identifier used for lookup operations.
     */
    @Column(unique = true, nullable = false)
    private String slug;

    /**
     * Human-readable description of the exercise.
     */
    @Column(nullable = false)
    private String description;

    /**
     * Exercise category (strength, cardio, or mobility).
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Category category;

    /**
     * Exercise difficulty level (beginner, intermediate, or advanced).
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    /**
     * Soft deletion flag. Only active exercises are returned by the API.
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Muscles involved in this exercise, including their impact level.
     * Lazy-loaded; must be initialized within an active persistence context.
     */
    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ExerciseMuscle> exerciseMuscles;
}
