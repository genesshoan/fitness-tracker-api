package dev.genesshoan.fitnesstrackerapi.stats.repository.projection;

/**
 * The aggregated training totals of a user over every completed session.
 *
 * @param totalWorkouts the number of completed workout sessions
 * @param totalSets the number of completed sets
 * @param totalReps the number of performed repetitions
 * @param totalVolumeKg the accumulated training volume, in kilogram-repetitions
 * @param totalTrainingSeconds the accumulated training time, in seconds
 * @param totalDistanceKm the accumulated distance, in kilometers
 * @param distinctExercises the number of distinct exercises trained
 */
public record TrainingMetricsProjection(
        long totalWorkouts,
        long totalSets,
        long totalReps,
        double totalVolumeKg,
        long totalTrainingSeconds,
        double totalDistanceKm,
        long distinctExercises) {}
