package dev.genesshoan.fitnesstrackerapi.routine.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request data for creating or fully updating a routine.
 *
 * <p>When exercises are supplied, they replace the routine's current exercise
 * list during an update. A missing exercise list is normalized to an empty one
 * so that creating a routine without exercises does not fail.
 */
@Schema(description = "Request DTO for creating a routine")
public record RoutineRequestDTO(
        @NotBlank @Schema(description = "The name of the routine", example = "Morning Routine")
        String name,

        @Schema(description = "The description of the routine", example = "A routine to get started in the morning")
        String description,

        @Valid
        @Schema(
                description = "The exercises in the routine",
                example = "[{\"exerciseId\": 1, \"sets\": 3, \"reps\": 10}]")
        List<@NotNull @Valid RoutineExerciseRequestDTO> exercises) {

    public RoutineRequestDTO {
        exercises = exercises == null ? List.of() : exercises;
    }
}
