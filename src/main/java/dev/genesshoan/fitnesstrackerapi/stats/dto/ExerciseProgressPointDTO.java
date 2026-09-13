package dev.genesshoan.fitnesstrackerapi.stats.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Exercise performance point for a completed workout session")
public record ExerciseProgressPointDTO(
        @Schema(description = "Date of the completed workout session", example = "2026-09-12")
        LocalDate date,

        @Schema(description = "Weight used in kilograms", example = "80.0")
        double weightKg,

        @Schema(description = "Number of repetitions", example = "8")
        int reps,

        @Schema(description = "Estimated one-repetition maximum in kilograms", example = "101.3")
        double estimatedOneRepMax) {}
