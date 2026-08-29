package dev.genesshoan.fitnesstrackerapi.workout.repository.projection;

import java.util.UUID;

public interface LastSetProjection {

    UUID getExerciseId();

    Integer getReps();

    Double getWeightKg();

    Integer getDurationSeconds();

    Double getDistanceKm();
}
