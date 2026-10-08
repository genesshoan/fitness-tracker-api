package dev.genesshoan.fitnesstrackerapi.stats.dto;

import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.BodyRegion;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API representation of one catalog muscle's range-relative intensity.
 */
@Schema(description = "Range-relative application intensity for one muscle")
public record MuscleIntensityDTO(
        @Schema(description = "Muscle id") UUID id,

        @Schema(description = "Muscle name", example = "Biceps")
        String name,

        @Schema(description = "Muscle slug", example = "biceps")
        String slug,

        @Schema(description = "Muscle body region", example = "ARMS")
        BodyRegion bodyRegion,

        @Schema(description = "Muscle front view asset") String frontAssetUrl,

        @Schema(description = "Muscle back view asset") String backAssetUrl,

        @Schema(description = "Relative intensity from 0.0 to 10.0", example = "10.0")
        double intensity) {}
