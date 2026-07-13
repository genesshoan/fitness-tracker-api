package dev.genesshoan.fitnesstrackerapi.routine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

@Schema(description = "Request DTO for creating a routine exercise")
public record RoutineExerciseRequestDTO(
        @NotNull(message = "Exercise id is required")
        @Schema(description = "Exercise id", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID exerciseId,

        @NotNull(message = "Default rest duration is required")
        @Min(value = 0, message = "Default rest duration must be non-negative")
        @Schema(description = "Default rest duration in seconds", example = "60")
        Integer defaultRestSeconds,

        @NotNull(message = "Default sets is required")
        @Positive(message = "Default sets must be positive")
        @Schema(description = "Default number of sets", example = "3")
        Integer defaultSets,

        @Schema(description = "Default number of reps", example = "10")
        Integer defaultReps,

        @Schema(description = "Default weight in kg", example = "50")
        Double defaultWeightKg,

        @Schema(description = "Default duration in seconds", example = "60")
        Integer defaultDurationSeconds,

        @Schema(description = "Default distance in km", example = "5.0")
        Double defaultDistanceKm,

        @Schema(description = "Notes", example = "None") String notes) {}
