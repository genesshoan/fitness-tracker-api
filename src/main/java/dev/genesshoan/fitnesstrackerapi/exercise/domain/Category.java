package dev.genesshoan.fitnesstrackerapi.exercise.domain;

import dev.genesshoan.fitnesstrackerapi.common.domain.ExerciseMetrics;

/**
 * Categorizes exercises by their primary movement type.
 *
 * <p>Each category defines validation rules for exercise metrics
 * and default metric values. Used for filtering exercises.
 */
public enum Category {
    STRENGTH {
        @Override
        public boolean validate(ExerciseMetrics metrics) {
            return metrics.reps() != null
                    && metrics.reps() > 0
                    && metrics.weightKg() != null
                    && metrics.weightKg() > 0
                    && metrics.distanceKm() == null
                    && metrics.durationSeconds() == null;
        }

        @Override
        public ExerciseMetrics defaultMetrics() {
            return new ExerciseMetrics(8, 5.0, null, null);
        }
    },

    CARDIO {
        @Override
        public boolean validate(ExerciseMetrics metrics) {
            return ((metrics.durationSeconds() != null && metrics.durationSeconds() > 0)
                            || (metrics.distanceKm() != null && metrics.distanceKm() > 0))
                    && metrics.reps() == null
                    && metrics.weightKg() == null;
        }

        @Override
        public ExerciseMetrics defaultMetrics() {
            return new ExerciseMetrics(null, null, 30, null);
        }
    },

    MOBILITY {
        @Override
        public boolean validate(ExerciseMetrics metrics) {
            return metrics.durationSeconds() != null
                    && metrics.durationSeconds() > 0
                    && metrics.reps() == null
                    && metrics.weightKg() == null
                    && metrics.distanceKm() == null;
        }

        @Override
        public ExerciseMetrics defaultMetrics() {
            return new ExerciseMetrics(null, null, 30, null);
        }
    };

    /**
     * Validates that exercise metrics match this category's requirements.
     *
     * @param metrics the exercise metrics to validate
     * @return true if the metrics match this category
     */
    public abstract boolean validate(ExerciseMetrics metrics);

    /**
     * Returns the default metric values for this category.
     *
     * @return default exercise metrics
     */
    public abstract ExerciseMetrics defaultMetrics();
}
