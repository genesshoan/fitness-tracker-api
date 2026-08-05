package dev.genesshoan.fitnesstrackerapi.exercise.domain;

import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineExerciseRequestDTO;

public enum Category {
    STRENGTH {
        @Override
        public boolean validate(RoutineExerciseRequestDTO dto) {
            return (dto.defaultReps() != null
                    && dto.defaultReps() > 0
                    && dto.defaultWeightKg() != null
                    && dto.defaultWeightKg() > 0
                    && dto.defaultDistanceKm() == null
                    && dto.defaultDurationSeconds() == null);
        }
    },

    CARDIO {
        @Override
        public boolean validate(RoutineExerciseRequestDTO dto) {
            return (((dto.defaultDurationSeconds() != null && dto.defaultDurationSeconds() > 0)
                            || (dto.defaultDistanceKm() != null && dto.defaultDistanceKm() > 0))
                    && dto.defaultReps() == null
                    && dto.defaultWeightKg() == null);
        }
    },

    MOBILITY {
        @Override
        public boolean validate(RoutineExerciseRequestDTO dto) {
            return (dto.defaultDurationSeconds() != null
                    && dto.defaultDurationSeconds() > 0
                    && dto.defaultReps() == null
                    && dto.defaultWeightKg() == null
                    && dto.defaultDistanceKm() == null);
        }
    };

    public abstract boolean validate(RoutineExerciseRequestDTO dto);
}
