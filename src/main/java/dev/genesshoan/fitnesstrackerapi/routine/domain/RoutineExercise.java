package dev.genesshoan.fitnesstrackerapi.routine.domain;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "routine_exercises",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_routine_exercise_position",
        columnNames = { "routine_id", "position" }
    )
)
public class RoutineExercise {

    @EmbeddedId
    private RoutineExerciseId id;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private int defaultSets;

    @Column
    private int defaultReps;

    @Column
    private int defaultWeightKg;

    @Column
    private int defaultDurationSeconds;

    @Column
    private int defaultDistanceMeters;

    @Column
    private String notes;

    @MapsId("routineId")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Routine routine;

    @MapsId("exerciseId")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Exercise exercise;
}
