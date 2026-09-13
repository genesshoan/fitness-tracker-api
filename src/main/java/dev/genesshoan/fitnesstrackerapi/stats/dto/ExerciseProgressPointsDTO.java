package dev.genesshoan.fitnesstrackerapi.stats.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Exercise progression over a date range")
public record ExerciseProgressPointsDTO(
        @Schema(description = "Exercise id") UUID exerciseId,

        @Schema(description = "Progress points ordered chronologically")
        List<ExerciseProgressPointDTO> progress) {}
