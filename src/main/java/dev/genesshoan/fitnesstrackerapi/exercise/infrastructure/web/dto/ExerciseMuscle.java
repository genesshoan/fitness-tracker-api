package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.web.dto;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Exercise muscle DTO")
public record ExerciseMuscle(
        @Schema(description = "Muscle") MuscleResponse muscle,

        @Schema(description = "Impact level", example = "PRIMARY")
        ImpactLevel impactLevel) {}
