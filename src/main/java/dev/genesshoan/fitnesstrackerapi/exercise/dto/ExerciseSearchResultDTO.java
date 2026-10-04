package dev.genesshoan.fitnesstrackerapi.exercise.dto;

import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lightweight exercise result for autocomplete suggestions.
 *
 * @param id exercise identifier
 * @param name exercise display name
 * @param slug URL-friendly exercise identifier
 * @param category exercise category
 * @param highlightedName name with the matching text wrapped in {@code <b>} tags
 */
@Schema(description = "Exercise autocomplete result")
public record ExerciseSearchResultDTO(
        @Schema(description = "Exercise ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Exercise display name", example = "Barbell Bench Press")
        String name,

        @Schema(description = "Exercise slug", example = "barbell-bench-press")
        String slug,

        @Schema(description = "Exercise category", example = "STRENGTH")
        Category category,

        @Schema(description = "Display name with the matching text highlighted", example = "<b>Barbell</b> Bench Press")
        String highlightedName) {}
