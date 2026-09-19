package dev.genesshoan.fitnesstrackerapi.exercise.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request data for creating or fully updating an exercise.
 *
 * <p>When muscles are supplied during an update, they replace the exercise's
 * current muscle associations.
 */
@Schema(description = "Request DTO for creating or updating an exercise")
public record ExerciseRequestDTO(
        @NotBlank @Schema(description = "Exercise name", example = "Bicep Curl")
        String name,

        @NotBlank @Schema(description = "URL-friendly identifier", example = "bicep-curl")
        String slug,

        @NotBlank @Schema(description = "Human-readable description", example = "Made with a bicep curl bar")
        String description,

        @Schema(description = "Step-by-step instructions for performing the exercise")
        List<String> instructions,

        @NotNull @Schema(description = "Exercise category", example = "STRENGTH")
        Category category,

        @NotNull @Schema(description = "Exercise difficulty", example = "INTERMEDIATE")
        Difficulty difficulty,

        @Schema(description = "Muscle associations; replace existing ones on update")
        List<@Valid ExerciseMuscleRequestDTO> muscles) {}
