package dev.genesshoan.fitnesstrackerapi.routine.dto;

import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseListItemDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The routine's exercise response")
public record RoutineExerciseResponseDTO(
        ExerciseListItemDTO exercise,
        @Schema(description = "The exercise's position", example = "1") int position,
        @Schema(description = "The exercise's default rest seconds", example = "60") int defaultRestSeconds,
        @Schema(description = "The exercise's default sets", example = "3") int defaultSets,
        @Schema(description = "The exercise's default reps", example = "10") Integer defaultReps,
        @Schema(description = "The exercise's default weight in kilograms", example = "100.0") Double defaultWeightKg,
        @Schema(description = "The exercise's default duration in seconds", example = "60")
                Integer defaultDurationSeconds,
        @Schema(description = "The exercise's default distance in kilometers", example = "0.0")
                Double defaultDistanceKm,
        @Schema(description = "The exercise's notes", example = "") String notes) {}
