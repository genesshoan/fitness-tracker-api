package dev.genesshoan.fitnesstrackerapi.workout.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import dev.genesshoan.fitnesstrackerapi.common.domain.ExerciseMetrics;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Session set request dto")
public record SessionSetRequestDTO(
        @Min(value = 1)
        @Schema(
                description = "Set number within the exercise. Used only when creating a session from scratch. "
                        + "When adding or updating a set on an existing session exercise, this field is ignored "
                        + "and the set keeps or receives the next sequential number.",
                example = "1")
        Integer setNumber,

        @Min(value = 1) @Schema(description = "Number of reps within the set", example = "12")
        Integer reps,

        @DecimalMin("1.0") @Schema(description = "Weight used in the exercise", example = "10")
        Double weightKg,

        @Min(value = 1) @Schema(description = "Set duration seconds", example = "5")
        Integer durationSeconds,

        @DecimalMin("1.0") @Min(value = 1) @Schema(description = "Distance covered in kilometers", example = "20.5")
        Double distanceKm,

        @NotNull @Schema(description = "Whether the set was completed")
        Boolean completed) {

    public ExerciseMetrics toExerciseMetrics() {
        return new ExerciseMetrics(reps, weightKg, durationSeconds, distanceKm);
    }

    public boolean isEmpty() {
        return reps == null && weightKg == null && durationSeconds == null && distanceKm == null;
    }
}
