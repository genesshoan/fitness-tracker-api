package dev.genesshoan.fitnesstrackerapi.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Strength volume aggregated for one calendar month in the user's timezone.
 *
 * @param month calendar month in {@code yyyy-MM} format
 * @param volumeKg total kilogram-repetitions for the month
 */
@Schema(description = "Monthly strength volume")
public record MonthlyVolumeDTO(
        @Schema(description = "Calendar month", example = "2026-09")
        String month,

        @Schema(description = "Total volume in kilogram-repetitions", example = "4320.0")
        double volumeKg) {}
