package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.web.dto;

import java.util.Set;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.BodyRegion;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Muscle response DTO")
public record MuscleResponse(
        @Schema(description = "Muscle name", example = "bicep")
        String name,

        @Schema(description = "Muscle slug", example = "bicep")
        String slug,

        @Schema(description = "Muscle body's region", example = "ARMS")
        BodyRegion bodyRegion,

        @Schema(description = "Muscle assets") Set<MuscleAssetResponse> assets) {}
