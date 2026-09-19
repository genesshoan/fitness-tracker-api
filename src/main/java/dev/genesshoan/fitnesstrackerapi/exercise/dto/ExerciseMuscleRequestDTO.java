package dev.genesshoan.fitnesstrackerapi.exercise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Muscle association in an exercise write request")
public record ExerciseMuscleRequestDTO(
        @NotBlank @Schema(description = "Slug of an existing muscle", example = "biceps")
        String muscleSlug,

        @NotNull @Schema(description = "Impact level of the muscle in the exercise", example = "PRIMARY")
        ImpactLevel impactLevel) {}
