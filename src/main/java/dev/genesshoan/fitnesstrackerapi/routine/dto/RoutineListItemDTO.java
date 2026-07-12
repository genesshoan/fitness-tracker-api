package dev.genesshoan.fitnesstrackerapi.routine.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A routine list item")
public record RoutineListItemDTO(
        @Schema(description = "The routine's unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
                UUID id,
        @Schema(description = "The routine's name", example = "Push Day") String name,
        @Schema(description = "The number of exercises in the routine", example = "5") int exerciseCount,
        @Schema(description = "The date and time the routine was last updated", example = "2024-01-01T12:00:00")
                LocalDateTime updatedAt) {}
