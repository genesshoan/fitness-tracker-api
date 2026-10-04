package dev.genesshoan.fitnesstrackerapi.stats.repository.projection;

/** JDBC projection for the weight and repetitions needed to calculate volume. */
public record VolumeSetProjection(Double weightKg, Integer reps) {}
