package dev.genesshoan.fitnesstrackerapi.stats.repository.projection;

/** JDBC projection for one exercise progression point. */
import java.time.Instant;

public record ExerciseProgressProjection(Instant instant, double weightKg, int reps, double estimatedOneRepMax) {}
