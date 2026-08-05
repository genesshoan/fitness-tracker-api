package dev.genesshoan.fitnesstrackerapi.testdata.builder;

import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.routine.domain.RoutineExercise;
import net.datafaker.Faker;

public class RoutineExerciseBuilder {

    private UUID id;
    private Integer position;
    private Integer defaultRestSeconds;
    private Integer defaultSets;
    private Integer defaultReps;
    private Double defaultWeightKg;
    private Integer defaultDurationSeconds;
    private Double defaultDistanceKm;
    private String notes;
    private Routine routine;
    private Exercise exercise;

    private RoutineExerciseBuilder(Faker faker) {
        this.id = UUID.randomUUID();
        this.position = 1;
        this.defaultRestSeconds = 60;
        this.defaultSets = 3;
        this.defaultReps = 12;
        this.defaultWeightKg = 14.0;
        this.defaultDurationSeconds = null;
        this.defaultDistanceKm = null;
        this.notes = faker.lorem().sentence();
    }

    public static RoutineExerciseBuilder aRoutineExercise(Faker faker) {
        return new RoutineExerciseBuilder(faker);
    }

    public RoutineExerciseBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public RoutineExerciseBuilder withPosition(Integer position) {
        this.position = position;
        return this;
    }

    public RoutineExerciseBuilder withDefaultRestSeconds(Integer defaultRestSeconds) {
        this.defaultRestSeconds = defaultRestSeconds;
        return this;
    }

    public RoutineExerciseBuilder withDefaultSets(Integer defaultSets) {
        this.defaultSets = defaultSets;
        return this;
    }

    public RoutineExerciseBuilder withDefaultReps(Integer defaultReps) {
        this.defaultReps = defaultReps;
        return this;
    }

    public RoutineExerciseBuilder withDefaultWeightKg(Double defaultWeightKg) {
        this.defaultWeightKg = defaultWeightKg;
        return this;
    }

    public RoutineExerciseBuilder withDefaultDurationSeconds(Integer defaultDurationSeconds) {
        this.defaultDurationSeconds = defaultDurationSeconds;
        return this;
    }

    public RoutineExerciseBuilder withDefaultDistanceKm(Double defaultDistanceKm) {
        this.defaultDistanceKm = defaultDistanceKm;
        return this;
    }

    public RoutineExerciseBuilder withNotes(String notes) {
        this.notes = notes;
        return this;
    }

    public RoutineExerciseBuilder forRoutine(Routine routine) {
        this.routine = routine;
        return this;
    }

    public RoutineExerciseBuilder forExercise(Exercise exercise) {
        this.exercise = exercise;
        return this;
    }

    public RoutineExercise build() {
        if (routine == null) {
            throw new IllegalStateException("Routine must be set to RoutineExercise");
        }
        if (exercise == null) {
            throw new IllegalStateException("Exercise must be set to RoutineExercise");
        }

        RoutineExercise routineExercise = RoutineExercise.builder()
                .id(id)
                .position(position)
                .defaultRestSeconds(defaultRestSeconds)
                .defaultSets(defaultSets)
                .defaultReps(defaultReps)
                .defaultWeightKg(defaultWeightKg)
                .defaultDurationSeconds(defaultDurationSeconds)
                .defaultDistanceKm(defaultDistanceKm)
                .notes(notes)
                .routine(routine)
                .exercise(exercise)
                .build();

        return routineExercise;
    }
}
