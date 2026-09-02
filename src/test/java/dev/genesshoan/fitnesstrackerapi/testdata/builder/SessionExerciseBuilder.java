package dev.genesshoan.fitnesstrackerapi.testdata.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import net.datafaker.Faker;

public class SessionExerciseBuilder {

    private UUID id;
    private int position = 1;
    private String notes;
    private WorkoutSession workoutSession;
    private Exercise exercise;
    private List<SessionSet> sets = new ArrayList<>();

    public SessionExerciseBuilder(Faker faker) {
        this.id = UUID.randomUUID();
        this.notes = faker.lorem().sentence();
    }

    public static SessionExerciseBuilder aSessionExercise(Faker faker) {
        return new SessionExerciseBuilder(faker);
    }

    public SessionExerciseBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public SessionExerciseBuilder withPosition(int position) {
        this.position = position;
        return this;
    }

    public SessionExerciseBuilder withNotes(String notes) {
        this.notes = notes;
        return this;
    }

    public SessionExerciseBuilder forWorkoutSession(WorkoutSession workoutSession) {
        this.workoutSession = workoutSession;
        return this;
    }

    public SessionExerciseBuilder forExercise(Exercise exercise) {
        this.exercise = exercise;
        return this;
    }

    public SessionExerciseBuilder withSets(List<SessionSet> sets) {
        this.sets = sets != null ? new ArrayList<>(sets) : new ArrayList<>();
        return this;
    }

    public SessionExerciseBuilder addSet(SessionSet set) {
        if (this.sets == null) {
            this.sets = new ArrayList<>();
        }
        this.sets.add(set);
        return this;
    }

    public SessionExercise build() {
        if (exercise == null) {
            throw new IllegalStateException("Exercise must be set to SessionExercise");
        }

        SessionExercise sessionExercise = SessionExercise.builder()
                .id(id)
                .position(position)
                .notes(notes)
                .exercise(exercise)
                .sets(sets)
                .build();

        if (workoutSession != null) {
            sessionExercise.setWorkoutSession(workoutSession);
        }

        if (sets != null) {
            sets.forEach(s -> s.setSessionExercise(sessionExercise));
        }

        return sessionExercise;
    }
}
