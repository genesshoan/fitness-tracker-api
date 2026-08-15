package dev.genesshoan.fitnesstrackerapi.workout.dto;

import com.sun.istack.NotNull;
import dev.genesshoan.fitnesstrackerapi.common.domain.ExerciseMetrics;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

@Schema(description = "Session set request dto")
public record SessionSetRequestDTO(
        @Min(value = 1)
        @Schema(
                description =
                        "Set number within the exercise. If omitted, the server assigns the next available number",
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
}
