package dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result;

import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;

/**
 * The summary of an exercise shown in a list of exercises.
 *
 * @param id           the exercise id
 * @param name         the exercise display name
 * @param slug         the URL-friendly exercise identifier
 * @param category     the exercise category
 * @param difficulty   the exercise difficulty
 * @param thumbnailUrl the public URL of the exercise thumbnail, or {@code null} when there is none
 */
public record ExerciseListItemResult(
        UUID id, String name, String slug, Category category, Difficulty difficulty, String thumbnailUrl) {}
