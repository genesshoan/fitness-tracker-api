package dev.genesshoan.fitnesstrackerapi.stats.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Range-relative muscle intensity visualization data")
public record MuscleIntensityResponseDTO(
        @Schema(description = "Inclusive start of the selected date range", example = "2026-09-01")
        LocalDate from,

        @Schema(description = "Inclusive end of the selected date range", example = "2026-09-30")
        LocalDate to,

        @Schema(description = "Every catalog muscle, including muscles with zero intensity")
        List<MuscleIntensityDTO> muscles) {}
