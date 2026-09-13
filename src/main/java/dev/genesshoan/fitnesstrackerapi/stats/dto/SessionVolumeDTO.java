package dev.genesshoan.fitnesstrackerapi.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Strength volume calculated for a workout session")
public record SessionVolumeDTO(
        @Schema(description = "Total volume in kilogram-repetitions", example = "4320.0")
        double volumeKg) {}
