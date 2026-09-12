package dev.genesshoan.fitnesstrackerapi.stats.repository.projection;

import java.util.UUID;

public record RankedSetProjection(
        UUID id,
        UUID sessionId,
        UUID exerciseId,
        Double weightKg,
        Integer reps,
        Double distanceKm,
        Integer durationSeconds,
        Integer rnWeight,
        Integer rnOneRepMax,
        Integer rnDistance,
        Integer rnDuration,
        Integer rnRepsPerWeight) {}
