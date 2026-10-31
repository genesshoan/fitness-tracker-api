package dev.genesshoan.fitnesstrackerapi.stats.application.port.in.result;

/**
 * The training aggregates of a user at a point in time.
 *
 * <p>Only completed sessions and completed sets are counted. The volume is the
 * sum of weight times repetitions, so a set without weight or without
 * repetitions contributes nothing to it.</p>
 *
 * <p>The streak is the longest run of consecutive days the user trained. It is
 * used instead of the current streak because a progress value has to be
 * monotonic, and the current streak drops back to zero when the user misses a
 * day.</p>
 *
 * @param totalWorkouts the number of completed workout sessions
 * @param totalSets the number of completed sets
 * @param totalReps the number of performed repetitions
 * @param totalVolumeKg the accumulated training volume, in kilogram-repetitions
 * @param totalTrainingSeconds the accumulated training time, in seconds
 * @param totalDistanceKm the accumulated distance, in kilometers
 * @param distinctExercises the number of distinct exercises trained
 * @param longestStreak the longest run of consecutive training days
 */
public record TrainingSnapshot(
        long totalWorkouts,
        long totalSets,
        long totalReps,
        double totalVolumeKg,
        long totalTrainingSeconds,
        double totalDistanceKm,
        long distinctExercises,
        int longestStreak) {}
