package dev.genesshoan.fitnesstrackerapi.routine.domain;

import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class RoutineExerciseId {

    UUID routineId;
    UUID exerciseId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoutineExerciseId)) return false;
        RoutineExerciseId that = (RoutineExerciseId) o;
        return (
            Objects.equals(this.routineId, that.routineId) &&
            Objects.equals(this.exerciseId, that.exerciseId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(routineId, exerciseId);
    }
}
