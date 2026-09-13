package dev.genesshoan.fitnesstrackerapi.exercise.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Composite primary key for the {@link ExerciseMuscle} join entity.
 *
 * <p>Consists of the exercise ID and muscle ID to uniquely
 * identify each exercise-muscle relationship.
 */
@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseMuscleId implements Serializable {

    /** The exercise ID. */
    private UUID exerciseId;

    /** The muscle ID. */
    private UUID muscleId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExerciseMuscleId)) return false;
        ExerciseMuscleId that = (ExerciseMuscleId) o;
        return (Objects.equals(exerciseId, that.exerciseId) && Objects.equals(muscleId, that.muscleId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(exerciseId, muscleId);
    }
}
