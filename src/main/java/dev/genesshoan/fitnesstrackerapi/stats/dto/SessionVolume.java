package dev.genesshoan.fitnesstrackerapi.stats.dto;

/** API representation of total strength volume for one workout session. */
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Strength volume calculated for a workout session")
public record SessionVolumeDTO(
        @Schema(description = "Total volume in kilogram-repetitions", example = "4320.0")
        double volumeKg) {}
