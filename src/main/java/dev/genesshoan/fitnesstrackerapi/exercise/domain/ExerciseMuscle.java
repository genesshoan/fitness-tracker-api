package dev.genesshoan.fitnesstrackerapi.exercise.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.Muscle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Links an exercise to a target muscle with an impact level.
 *
 * <p>This is a join entity for the many-to-many relationship
 * between Exercise and Muscle. It stores how significantly
 * each muscle is involved in the exercise:
 *
 * <ul>
 *   <li>PRIMARY - the main target muscle</li>
 *   <li>SECONDARY - supporting muscle</li>
 *   <li>STABILIZER - muscle that stabilizes the movement</li>
 * </ul>
 *
 * The composite key ({@link ExerciseMuscleId}) consists of
 * exerciseId and muscleId.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "exercise_muscles")
public class ExerciseMuscle {

    @EmbeddedId
    private ExerciseMuscleId id;

    /**
     * The impact level of this muscle in the exercise.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ImpactLevel impactLevel;

    /**
     * The target muscle. Lazy-loaded; must be initialized within
     * an active persistence context.
     */
    @MapsId("muscleId")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "muscle_id", nullable = false)
    private Muscle muscle;

    /**
     * The exercise. Lazy-loaded; must be initialized within
     * an active persistence context.
     */
    @MapsId("exerciseId")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;
}
