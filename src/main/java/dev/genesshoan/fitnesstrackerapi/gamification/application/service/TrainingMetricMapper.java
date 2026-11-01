package dev.genesshoan.fitnesstrackerapi.gamification.application.service;

import org.springframework.stereotype.Component;

import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementMetric;
import dev.genesshoan.fitnesstrackerapi.stats.application.port.in.result.TrainingSnapshot;

/**
 * Reads the value of a threshold metric out of a training snapshot.
 *
 * <p>This is the seam between the metrics the gamification module reasons
 * about and the aggregates the statistics module computes.</p>
 */
@Component
public class TrainingMetricMapper {

    /**
     * Returns the value of the metric in the snapshot.
     *
     * @param metric the metric to read
     * @param snapshot the training totals of the user
     * @return the value of the metric
     * @throws IllegalArgumentException when the metric is not a threshold metric
     */
    public double valueOf(AchievementMetric metric, TrainingSnapshot snapshot) {
        return switch (metric) {
            case TOTAL_WORKOUTS -> snapshot.totalWorkouts();
            case LONGEST_STREAK -> snapshot.longestStreak();
            case TOTAL_SETS -> snapshot.totalSets();
            case TOTAL_REPS -> snapshot.totalReps();
            case TOTAL_VOLUME_KG -> snapshot.totalVolumeKg();
            case TOTAL_TRAINING_SECONDS -> snapshot.totalTrainingSeconds();
            case TOTAL_DISTANCE_KM -> snapshot.totalDistanceKm();
            case DISTINCT_EXERCISES -> snapshot.distinctExercises();
            default ->
                throw new IllegalArgumentException(
                        "The metric " + metric + " is not a threshold metric and has no value in a training snapshot.");
        };
    }
}
