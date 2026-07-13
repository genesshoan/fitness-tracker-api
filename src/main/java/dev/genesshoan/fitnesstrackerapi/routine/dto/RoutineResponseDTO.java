package dev.genesshoan.fitnesstrackerapi.routine.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The routine's response")
public record RoutineResponseDTO(
        @Schema(description = "The routine's unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "The routine's name", example = "Push Day")
        String name,

        @Schema(description = "The routine's description", example = "A routine for pushing heavy weights")
        String description,

        @Schema(
                description = "The routine's exercises",
                example =
                        "[{\"exerciseId\": \"abc123\", \"name\": \"Bench Press\", \"defaultWeightKg\": 100.0, \"defaultDurationSeconds\": 60, \"defaultDistanceKm\": 0.0, \"notes\": \"\"}]")
        List<RoutineExerciseResponseDTO> exercises) {}
