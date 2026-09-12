package dev.genesshoan.fitnesstrackerapi.stats.repository.projection;

import java.time.Instant;

public record ExerciseProgressProjection(Instant instant, double weightKg, int reps, double estimatedOneRepMax) {}
