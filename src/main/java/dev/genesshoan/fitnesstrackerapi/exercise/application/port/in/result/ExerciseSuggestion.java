package dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result;

import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;

/**
 * An exercise suggestion returned by the catalog search.
 *
 * @param id             the exercise id
 * @param name           the exercise display name
 * @param slug           the URL-friendly exercise identifier
 * @param category       the exercise category
 * @param highlightedName the display name with the matching text wrapped in {@code <b>} tags
 */
public record ExerciseSuggestion(UUID id, String name, String slug, Category category, String highlightedName) {}
