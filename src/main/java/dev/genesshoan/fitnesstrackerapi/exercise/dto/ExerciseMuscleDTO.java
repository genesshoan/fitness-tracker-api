package dev.genesshoan.fitnesstrackerapi.exercise.dto;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.dto.MuscleResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Exercise muscle DTO")
public record ExerciseMuscleDTO(
    @Schema(description = "Muscle") MuscleResponseDTO muscle,
    @Schema(description = "Impact level", example = "PRIMARY")
    ImpactLevel impactLevel
) {}
