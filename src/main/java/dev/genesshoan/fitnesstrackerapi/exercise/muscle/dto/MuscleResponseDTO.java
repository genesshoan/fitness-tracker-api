package dev.genesshoan.fitnesstrackerapi.exercise.muscle.dto;

import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.BodyRegion;
import io.swagger.v3.oas.annotations.media.Schema;

public record MuscleResponseDTO(

        @Schema(description = "Muscle name", example = "bicep")
        String name,

        @Schema(description = "Muscle body's region", example = "ARMS")
        BodyRegion bodyRegion
) {
}
