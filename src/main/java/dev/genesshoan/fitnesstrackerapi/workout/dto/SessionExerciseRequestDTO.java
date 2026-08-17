package dev.genesshoan.fitnesstrackerapi.workout.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Session exercise request dto")
public record SessionExerciseRequestDTO(
        @Min(value = 1)
        @Schema(
                description = "1-based position where the exercise will be inserted. "
                        + "If omitted, the exercise is appended to the end.")
        Integer position,

        @Schema(description = "Session exercise notes", example = "Focus on controlled eccentric")
        String notes,

        @NotNull @Schema(description = "Exercise performed in this session")
        UUID exerciseId,

        @Valid @NotNull @Size(min = 1) @Schema(description = "Sets performed for the exercise")
        List<SessionSetRequestDTO> sets) {}
