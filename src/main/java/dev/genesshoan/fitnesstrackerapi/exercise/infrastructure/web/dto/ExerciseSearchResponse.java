package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.web.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Autocomplete response containing matching active exercises.
 *
 * @param results matching exercise suggestions
 */
@Schema(description = "Exercise autocomplete response")
public record ExerciseSearchResponse(
        @Schema(description = "Matching exercise suggestions")
        List<ExerciseSearchResult> results) {}
