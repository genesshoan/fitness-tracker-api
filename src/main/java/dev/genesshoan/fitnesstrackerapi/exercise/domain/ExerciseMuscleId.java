package dev.genesshoan.fitnesstrackerapi.exercise.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseMuscleId implements Serializable {

    private Long exerciseId;
    private Long muscleId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExerciseMuscleId)) return false;
        ExerciseMuscleId that = (ExerciseMuscleId) o;
        return (
            Objects.equals(exerciseId, that.exerciseId) &&
            Objects.equals(muscleId, that.muscleId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(exerciseId, muscleId);
    }
}
