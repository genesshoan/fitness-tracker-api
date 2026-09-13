package dev.genesshoan.fitnesstrackerapi.stats.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estimated one-repetition maximum for an exercise")
public record OneRepMaxDTO(
        @Schema(description = "Exercise id") UUID exerciseId,

        @Schema(description = "Estimated one-repetition maximum in kilograms", example = "101.3")
        double estimatedOneRepMax) {}
