package dev.genesshoan.fitnesstrackerapi.exercise.domain;

import dev.genesshoan.fitnesstrackerapi.common.domain.ExerciseMetrics;

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

    public abstract boolean validate(ExerciseMetrics metrics);

    public abstract ExerciseMetrics defaultMetrics();
}
