package dev.genesshoan.fitnesstrackerapi.workout.dto;

import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseListItemDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Session exercise respones dto")
public record SessionExerciseResponseDTO(
        @Schema(description = "Session exercise id")
        UUID id,

        @Schema(description = "Exercise position within a workout session", example = "1")
        int position,

        @Schema(description = "Session exercise notes", example = "Focus on controlled eccentric")
        String notes,

        @Schema(description = "Exercise performed in this session")
        ExerciseListItemDTO exercise,

        @Schema(description = "Sets performed for the exercise")
        List<SessionSetResponseDTO> sets) {}
