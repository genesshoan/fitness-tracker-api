package dev.genesshoan.fitnesstrackerapi.testdata.builder;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ExerciseMuscle;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ExerciseMuscleId;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.Muscle;

public class ExerciseMuscleBuilder {

    private Exercise exercise;
    private Muscle muscle;
    private ImpactLevel impactLevel = ImpactLevel.PRIMARY;

    public static ExerciseMuscleBuilder anExerciseMuscle() {
        return new ExerciseMuscleBuilder();
    }

    public ExerciseMuscleBuilder forExercise(Exercise exercise) {
        this.exercise = exercise;
        return this;
    }

    public ExerciseMuscleBuilder forMuscle(Muscle muscle) {
        this.muscle = muscle;
        return this;
    }

    public ExerciseMuscleBuilder withImpact(ImpactLevel impactLevel) {
        this.impactLevel = impactLevel;
        return this;
    }

    public ExerciseMuscle build() {
        if (exercise == null || muscle == null) {
            throw new IllegalStateException(
                "Exercise and Muscle must be set for ExerciseMuscle."
            );
        }

        ExerciseMuscleId id = new ExerciseMuscleId(
            exercise.getId(),
            muscle.getId()
        );

        return ExerciseMuscle.builder()
            .id(id)
            .exercise(exercise)
            .muscle(muscle)
            .impactLevel(impactLevel)
            .build();
    }
}
