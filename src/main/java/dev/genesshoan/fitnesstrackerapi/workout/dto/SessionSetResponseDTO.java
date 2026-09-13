package dev.genesshoan.fitnesstrackerapi.workout.dto;

import java.util.List;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.common.domain.ExerciseMetrics;
import dev.genesshoan.fitnesstrackerapi.stats.dto.AchievementDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Session set response dto")
public record SessionSetResponseDTO(
        @Schema(description = "Session set id") UUID id,

        @Schema(description = "Set number within the exercise", example = "1")
        int setNumber,

        @Schema(description = "Number of reps within the set", example = "12")
        Integer reps,

        @Schema(description = "Weight used in the exercise", example = "10")
        Double weightKg,

        @Schema(description = "Set duration seconds", example = "5")
        Integer durationSeconds,

        @Schema(description = "Distance covered in kilometers", example = "20.5")
        Double distanceKm,

        @Schema(description = "Whether the set was completed")
        boolean completed,

        @Schema(description = "Achievements obtained by this set")
        List<AchievementDTO> achievements) {

    public ExerciseMetrics toExerciseMetrics() {
        return new ExerciseMetrics(reps, weightKg, durationSeconds, distanceKm);
    }
}
