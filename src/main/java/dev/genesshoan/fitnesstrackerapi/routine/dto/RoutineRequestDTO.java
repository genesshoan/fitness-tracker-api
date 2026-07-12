package dev.genesshoan.fitnesstrackerapi.routine.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request DTO for creating a routine")
public record RoutineRequestDTO(
        @NotBlank @Schema(description = "The name of the routine", example = "Morning Routine") String name,
        @Schema(description = "The description of the routine", example = "A routine to get started in the morning")
                String description,
        @NotEmpty
                @Schema(
                        description = "The exercises in the routine",
                        example = "[{\"exerciseId\": 1, \"sets\": 3, \"reps\": 10}]")
                List<RoutineExerciseRequestDTO> exercises) {}
