package dev.genesshoan.fitnesstrackerapi.testdata.builder;

import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;
import net.datafaker.Faker;

public class SessionSetBuilder {

    private UUID id;
    private int setNumber = 1;
    private Integer reps = 12;
    private Double weightKg = 14.0;
    private Integer durationSeconds;
    private Double distanceKm;
    private boolean completed = false;
    private SessionExercise sessionExercise;

    public SessionSetBuilder(Faker faker) {
        this.id = UUID.randomUUID();
    }

    public static SessionSetBuilder aSessionSet(Faker faker) {
        return new SessionSetBuilder(faker);
    }

    public SessionSetBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public SessionSetBuilder withSetNumber(int setNumber) {
        this.setNumber = setNumber;
        return this;
    }

    public SessionSetBuilder withReps(Integer reps) {
        this.reps = reps;
        return this;
    }

    public SessionSetBuilder withWeightKg(Double weightKg) {
        this.weightKg = weightKg;
        return this;
    }

    public SessionSetBuilder withDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
        return this;
    }

    public SessionSetBuilder withDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
        return this;
    }

    public SessionSetBuilder withCompleted(boolean completed) {
        this.completed = completed;
        return this;
    }

    public SessionSetBuilder forSessionExercise(SessionExercise sessionExercise) {
        this.sessionExercise = sessionExercise;
        return this;
    }

    public SessionSet build() {
        SessionSet sessionSet = SessionSet.builder()
                .id(id)
                .setNumber(setNumber)
                .reps(reps)
                .weightKg(weightKg)
                .durationSeconds(durationSeconds)
                .distanceKm(distanceKm)
                .completed(completed)
                .build();

        if (sessionExercise != null) {
            sessionSet.setSessionExercise(sessionExercise);
        }

        return sessionSet;
    }
}
